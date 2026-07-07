# ADR 0001 — Convenção de contagem de prazo (day-count)

## Contexto

A fórmula de valor presente usa uma taxa **mensal** (base + spread). O prazo
entre a data de referência (liquidação/simulação) e o vencimento do recebível
é dado em dias corridos no banco. Sem uma convenção explícita, dois
desenvolvedores calculariam o expoente da fórmula de juros compostos de
formas diferentes — esse é o erro clássico deste tipo de motor.

## Decisão

Prazo em meses = `dias corridos entre referenceDate e dueDate ÷ 30`.

- Dias corridos, não dias úteis.
- Mês comercial fixo de 30 dias — não é a convenção bancária 30/360 completa
  (que trata meses de 31 dias e fim de mês de forma especial), é a divisão
  simples `dias / 30`.
- O resultado é fracionário (ex.: 45 dias = 1,5 mês), por isso a fórmula de
  juros compostos precisa de expoente fracionário (ver ADR 0002).
- Implementado em `TermCalculator.monthsBetween` — ponto único, sem
  duplicação da conta em outro lugar do código.

## Consequências

- Simples de auditar e de explicar a um usuário de negócio.
- Se o requisito real precisar de 30/360 bancário ou dias úteis, é uma
  mudança isolada em `TermCalculator`, sem tocar nas Strategies.
- `TermCalculator` rejeita `dueDate` anterior a `referenceDate`
  (`IllegalArgumentException`) em vez de silenciosamente produzir um prazo
  negativo.
