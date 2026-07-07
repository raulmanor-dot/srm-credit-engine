# ADR 0005 — `PricingStrategy` + resolver via Spring (Open/Closed)

## Contexto

O motor precisa suportar múltiplos tipos de recebível, cada um com sua
própria regra de precificação, sem que adicionar um tipo novo exija editar
um `switch`/`if-else` central (violação de Open/Closed e ponto único de
regressão a cada tipo novo).

## Decisão

- `PricingStrategy` é a interface (`getType()`,
  `calculatePresentValue(Receivable, BaseRate)`).
- `PricingStrategyResolver` recebe `List<PricingStrategy>` via injeção de
  construtor do Spring — todo bean `PricingStrategy` no contexto entra
  automaticamente no mapa `ReceivableTypeCode -> PricingStrategy`. Não há
  `if/else` nem `switch` em lugar nenhum decidindo qual implementação usar.
- `DuplicataMercantilStrategy` e `ChequePreDatadoStrategy` compartilham a
  fórmula de juros compostos via `AbstractCompoundInterestPricingStrategy`
  (elas diferem hoje só no `getType()` e no spread, que vem do banco — ver
  README). Um tipo futuro com fórmula genuinamente diferente (juros simples,
  por exemplo) implementa `PricingStrategy` diretamente, sem tocar nas
  classes existentes nem no resolver.
- `BaseRate` é um `record` imutável (`monthlyPercent`, `referenceDate`) —
  a Strategy nunca chama `LocalDate.now()` internamente, o que manteria o
  cálculo puro e determinístico (mesmo input, mesmo output), facilitando
  testes sem mocks de tempo.

## Consequências

- Adicionar um tipo de recebível = 1 migration (linha em `receivable_types`)
  + 1 classe `@Component`. Nenhum arquivo existente muda.
- Se `strategiesByType` não tiver o tipo (erro de configuração/dado),
  `PricingStrategyResolver.resolve` lança
  `UnsupportedReceivableTypeException` explícita em vez de `NullPointerException`.
