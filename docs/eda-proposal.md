# Proposta de Arquitetura Orientada a Eventos (EDA)

Entregável de nível Especialista/Staff (item 6 do enunciado): modelagem de eventos para o
SRM Credit Engine, complementar ao [design de alta escala](./high-scale-design.md). O
objetivo é desacoplar efeitos colaterais do caminho crítico de liquidação (que hoje é
inteiramente síncrono, dentro de uma única transação Postgres) sem enfraquecer a garantia
ACID que protege o dinheiro em si.

## Por que EDA aqui, e onde parar

A liquidação em si — debitar o recebível, calcular o valor líquido, gravar o registro —
**continua transacional e síncrona**, dentro do Postgres, exatamente como hoje. Isso não
muda: é a parte do sistema onde consistência forte importa mais do que desempenho.

O que vira evento são os efeitos **posteriores** à liquidação já confirmada: notificar,
alimentar relatórios, atualizar métricas, dar baixa em sistemas externos. Hoje esses
efeitos, quando existem, competem pelo mesmo tempo de resposta da requisição HTTP; como
eventos, passam a acontecer de forma assíncrona, sem atrasar a confirmação ao operador.

## Outbox transacional

Publicar um evento diretamente para um tópico *depois* do commit do banco tem uma janela
de falha real: o commit pode ter sucesso e a publicação falhar (ou vice-versa), quebrando
a garantia de que "toda liquidação gera exatamente um evento". A solução padrão —
**Transactional Outbox** — resolve isso sem exigir um coordenador de transação distribuída:

1. A liquidação grava a linha em `settlements` **e** uma linha em uma tabela
   `outbox_events` **na mesma transação Postgres** (mesmo commit atômico).
2. Um processo separado (poller ou Debezium via CDC no WAL do Postgres) lê
   `outbox_events` e publica no broker, marcando cada linha como publicada.
3. Se o processo de publicação cair, ele retoma de onde parou na próxima execução — o
   evento nunca se perde, porque nunca dependeu de uma segunda transação distribuída para
   existir.

Isso mantém o mesmo princípio já aplicado em `exchange_rates` (append-only, auditável) e
em `settlements` (UNIQUE + optimistic lock): a garantia vive no banco, não na aplicação.

## Eventos propostos

| Evento | Disparado por | Consumidores plausíveis |
|---|---|---|
| `ReceivableRegistered` | Cadastro de um novo recebível (`status = PENDING`) | Sistemas de risco/compliance que monitoram exposição por cedente |
| `SettlementRequested` | Início do fluxo de liquidação, antes da confirmação | Auditoria de tentativas (inclusive as que falham por 409/422) |
| `SettlementCompleted` | Liquidação confirmada (commit da transação) | Relatórios/BI, notificação ao operador, baixa em sistemas contábeis externos |
| `FxRateUpdated` | Nova cotação persistida em `exchange_rates` | Cache L1/L2 de cotação (invalidação proativa, ver design de alta escala), dashboards de câmbio |

`SettlementCompleted` carrega o mesmo payload já auditável hoje em `settlements` (taxa
base, spread, câmbio usado, valores em ambas moedas) — o evento não introduz um novo
formato de dado, apenas transporta o que já é persistido.

## Tópicos e particionamento

- Broker: Kafka (ou equivalente com garantia de ordenação por partição e retenção
  configurável — a escolha específica é menos importante que a garantia).
- **Chave de partição = `assignor_id`** — mesma chave usada no sharding proposto no
  [design de alta escala](./high-scale-design.md#4-sharding). Eventos do mesmo cedente
  chegam ordenados ao consumidor; cedentes diferentes processam em paralelo sem
  coordenação entre si.
- `FxRateUpdated` particiona por par de moeda (`base_currency_id`/`quote_currency_id`),
  já que não tem relação com cedente.

## Garantias de entrega e consumo

- **At-least-once**: o outbox garante que todo evento é publicado pelo menos uma vez; um
  consumidor pode, em tese, receber o mesmo evento duas vezes (reentrega após falha de
  ack). Consumidores devem ser **idempotentes** — o próprio evento carrega o `settlementId`
  como chave natural de deduplicação, o mesmo identificador que já é `UNIQUE` no banco de
  origem.
- **Ordenação por partição**: garantida pela chave de particionamento (`assignor_id`);
  não há garantia de ordem global entre partições diferentes, o que é aceitável — eventos
  de cedentes diferentes não têm relação causal entre si.
- Consumidores lentos ou fora do ar não bloqueiam a liquidação em si (que já commitou
  antes do evento existir) — a "fila" que se acumula é a do broker, não a do usuário.

## O que isso NÃO muda

- A liquidação continua ACID, síncrona, dentro de uma única transação Postgres.
- `exchange_rates` continua append-only por trigger de banco — `FxRateUpdated` é um efeito
  colateral do insert, não um substituto dele.
- Nenhum dado financeiro passa a existir "só" no broker — o Postgres continua sendo a
  fonte de verdade; o broker transporta notificação de fatos já commitados.
