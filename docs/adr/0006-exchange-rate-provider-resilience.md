# ADR 0006 — Provedor de câmbio mock com Resilience4j (retry/circuit breaker/fallback)

## Contexto

O motor de liquidação precisa de uma cotação de câmbio para converter entre
a moeda de face do recebível e a moeda de pagamento. Até aqui essa cotação
vinha só do banco (uma linha seedada manualmente). Um provedor externo real
de câmbio pode ficar lento ou fora do ar — a liquidação não pode travar por
isso, mas também não pode ficar martelando um provedor que já está caído.
O escopo do desafio pede explicitamente um provedor mock protegido por
retry/circuit breaker, com um caminho de fallback.

## Decisão

- **Porta no domínio, adapter na infraestrutura.** `ExchangeRateProvider`
  (`domain/fx`) é a única coisa que `ExchangeRateService` conhece. O cliente
  HTTP, o `RestClient` e as anotações do Resilience4j vivem em
  `infrastructure/fx/HttpExchangeRateProviderClient` — o domínio nunca
  depende de Resilience4j ou de detalhes de transporte.
- **O provedor mock é um endpoint HTTP dentro da própria aplicação**
  (`GET /mock-provider/rates?base=..&quote=..`, pacote `mockprovider`),
  chamado via `RestClient` real (não um stub em memória). Isso faz o
  retry/circuit breaker exercitarem timeout e falha de rede de verdade, não
  uma simulação em memória. Caos (taxa de falha, latência artificial, jitter
  na cotação) é configurável via `app.mock-provider.*`.
- **Resilience4j anotado**: `@Retry` (mais externo, com fallback) e
  `@CircuitBreaker` no método `fetchRate`. O circuito registra cada
  tentativa; quando abre, `CallNotPermittedException` está na lista de
  `ignore-exceptions` do retry, para falhar rápido direto no fallback em vez
  de tentar de novo contra um circuito já aberto.
- **Escada de degradação**: taxa fresca do provedor (persistida como nova
  linha append-only, `source = MOCK_PROVIDER`, ver ADR 0003) → se o
  provedor estiver indisponível (retries esgotados ou circuito aberto),
  última taxa conhecida no banco (com log de warning) → `422` só se nunca
  existiu nenhuma taxa para o par. A liquidação nunca é bloqueada pela
  indisponibilidade de um provedor externo.
- A chamada ao provedor acontece dentro da própria transação `REQUIRES_NEW`
  de `SettlementService.settle`. Pior caso limitado (~6,6s: 3 tentativas ×
  2s de read-timeout + backoff), aceitável porque a liquidação em lote já é
  sequencial por item (ADR 0004).

## Alternativas consideradas

- **Container WireMock separado no compose**: mais fiel a um provedor
  externo de verdade, mas adiciona um serviço a mais para o avaliador subir,
  sem ganho real sobre o endpoint mock in-process — descartado.
- **Bean fake sem HTTP**: mais simples, mas não exercitaria timeout/erro de
  rede de verdade, tornando o retry/circuit breaker decorativos.
- **Buscar e persistir a taxa numa transação `REQUIRES_NEW` separada**:
  isolaria a cotação persistida de um rollback da liquidação, mas adiciona
  uma seam transacional para um ganho marginal — rejeitado como
  over-engineering para este escopo.

## Consequências

- `exchange_rates` ganha uma linha nova a cada liquidação cross-currency com
  o provedor habilitado — esperado, é o próprio modelo append-only (ADR
  0003).
- Em modo de fallback, a liquidação pode usar uma taxa desatualizada — isso
  é logado como warning, e `settlements.fx_rate_used` preserva o valor
  exato efetivamente usado no snapshot de auditoria.
- Estado do circuito é observável via `/actuator/circuitbreakers` — ponto de
  apoio já pronto para a fase de observabilidade (Micrometer/Prometheus).
- Testes de integração que provocam o comportamento do provedor usam pares
  de moeda dedicados (EUR/JPY, GBP/CHF) em vez de USD/BRL, para não
  contaminar com uma cotação "fresca" o par que outros testes assumem estar
  fixo no valor seedado pela V7 — consequência direta de `exchange_rates`
  ser append-only num Postgres de teste compartilhado por toda a suíte.
