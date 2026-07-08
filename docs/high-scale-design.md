# Design de Alta Escala — 1 milhão de transações/minuto

Entregável de nível Especialista/Staff (item 6 do enunciado): como a arquitetura atual
evoluiria para sustentar ~16.700 liquidações/segundo em produção, sem reescrever o
domínio já validado (Strategy de pricing, optimistic locking, append-only de câmbio).

O sistema atual (um único Postgres + uma instância Spring Boot) é deliberadamente simples
— correto para o escopo do desafio, mas com gargalos conhecidos e explícitos abaixo. O
objetivo aqui não é "reescrever tudo", e sim identificar **onde** cada gargalo apareceria
primeiro e qual técnica o resolve, mantendo os invariantes de negócio (ACID na liquidação,
precisão decimal, auditabilidade) intactos.

## Gargalos do design atual, em ordem de que apareceria primeiro

1. **Uma única instância da API** — sem stateless scaling horizontal configurado.
2. **Um único Postgres primário** — todo write e todo read (inclusive o extrato analítico)
   caem na mesma instância.
3. **Chamada síncrona ao provedor de câmbio dentro da transação de liquidação** — o
   Resilience4j já protege contra falha, mas a latência de rede ainda acontece *dentro*
   do tempo de resposta do usuário.
4. **Optimistic locking com retry implícito do cliente** — funciona bem em baixa
   contenção; sob alta concorrência no mesmo recebível (incomum, mas não impossível em
   lote), gera uma fila de 409 e retries.

## Estratégia por camada

### 1. Camada de API — stateless horizontal scaling

A API já não guarda estado em memória entre requisições (nenhuma sessão HTTP, JWT/stateless
seria o caminho de autenticação). Isso permite rodar N réplicas atrás de um load balancer
sem mudança de código — apenas infraestrutura (ver [docs/eda-proposal.md](./eda-proposal.md)
para o desacoplamento que reduz ainda mais a carga por instância).

### 2. Cache de taxas de câmbio

A cotação vigente (linha mais recente de `exchange_rates` por par de moeda) é lida em
praticamente toda simulação e liquidação cross-currency. Hoje é um `SELECT` a cada
chamada. Em escala:

- **Cache local (Caffeine) com TTL curto (poucos segundos)** dentro de cada instância da
  API, invalidado por tempo — a cotação já é, por natureza, um valor que expira.
  Elimina a maior parte das leituras repetidas sem introduzir uma dependência de rede
  extra (Redis) para um dado que já é, por design, append-only e barato de recarregar.
- Se o volume justificar um cache compartilhado entre instâncias (evitar N caches
  frios simultâneos), evoluir para **Redis** como camada L2 — mas só se a métrica de
  cache-miss no Caffeine mostrar necessidade; não adicionar a dependência antes de medir.

### 3. Read replicas e separação de carga analítica

O próprio código já modela essa separação: `ReportController` fala direto com
`SettlementReportRepository` via SQL nativo, sem passar pela camada de domínio — esse é
exatamente o caminho que se aponta para uma **read replica dedicada a relatórios**. Em
escala:

- Escritas (liquidação, criação de recebível) continuam no primário.
- O extrato de liquidação (`GET /reports/settlements`) passa a ler de uma ou mais réplicas
  de leitura, aceitando **consistência eventual** (replicação assíncrona do Postgres) — um
  relatório que reflete o estado de alguns milissegundos atrás é uma troca aceitável para
  esse caso de uso; a liquidação em si nunca lê da réplica.

### 4. Sharding

Para volume de escrita além do que um único primário Postgres sustenta, a chave de
particionamento natural é **`assignor_id`** (cedente): liquidações de cedentes diferentes
não têm nenhuma dependência transacional entre si (cada liquidação já é uma transação
isolada por recebível). Particionar por `assignor_id` (hash ou range, dependendo da
distribuição real de volume por cedente) permite:

- Escalar horizontalmente o armazenamento de `receivables`/`settlements`.
- Manter a transação ACID de uma liquidação inteiramente dentro de um único shard —
  nenhuma liquidação individual precisa de transação distribuída entre shards.
- O extrato agregado (multi-cedente) passa a ser um fan-out de leitura entre shards,
  reforçando por que essa rota já vive fora da camada de domínio.

`exchange_rates` não é particionado por cedente (é global por par de moeda) — continua
replicado inteiro em cada shard, dado seu volume ser ordens de magnitude menor.

### 5. Assincronizar a chamada ao provedor de câmbio

Hoje a busca de cotação acontece de forma síncrona dentro do fluxo de liquidação (com
retry + circuit breaker do Resilience4j). Para reduzir a latência percebida pelo operador
em alta escala, a cotação usada na liquidação pode passar a ser **sempre a última já
persistida em `exchange_rates`** (nunca uma chamada de rede síncrona no caminho crítico),
com um processo assíncrono separado responsável por mantê-la atualizada — o
[ADR 0006](./adr/0006-exchange-rate-provider-resilience.md) já trata a busca síncrona como
best-effort com fallback para o valor em banco; em escala, o valor em banco deixa de ser o
fallback e passa a ser o caminho principal, e a atualização de cotação vira um evento (ver
[docs/eda-proposal.md](./eda-proposal.md)).

### 6. Idempotência e rate limiting na borda

Em 1M tx/min, retries de cliente (timeout do lado do operador, retry automático de
integração) precisam ser seguros. A constraint `UNIQUE(settlements.receivable_id)` já
garante que uma segunda tentativa de liquidar o mesmo recebível falha com 409 em vez de
duplicar — a extensão necessária em escala é aceitar uma **chave de idempotência** por
requisição (header `Idempotency-Key`), permitindo ao cliente reenviar com segurança e
receber a mesma resposta da primeira tentativa bem-sucedida, sem depender só da constraint
de banco para detectar duplicidade. Rate limiting (por cliente/token) na borda (API
gateway ou filtro na própria aplicação) protege contra picos que excedam a capacidade
provisionada, devolvendo 429 de forma controlada em vez de degradar a latência de todos.

## Resumo do que muda vs. o que não muda

| Continua igual | Muda em escala |
|---|---|
| Motor de precificação (Strategy) | API roda em N réplicas stateless |
| `NUMERIC(19,6)` / BigDecimal | Cache L1 (Caffeine) de cotação de câmbio |
| Optimistic lock + UNIQUE constraint na liquidação | Read replica dedicada ao extrato |
| Append-only de `exchange_rates` | Sharding de `receivables`/`settlements` por `assignor_id` |
| ACID por liquidação individual | Cotação de câmbio via evento assíncrono, não chamada síncrona |
| | Idempotency-Key + rate limiting na borda |
