# ADR 0002 — Precisão numérica e potência fracionária

## Contexto

O valor presente é `FV / (1 + i)^t`, onde `t` (prazo em meses, ver ADR 0001)
é fracionário. Duas armadilhas comuns:

1. `double`/`Math.pow` introduzem erro de arredondamento binário — inaceitável
   em cálculo financeiro auditável (diferenças de centavos que não fecham em
   reconciliação).
2. `BigDecimal.pow(int)` só aceita expoente **inteiro** — não resolve o caso
   de prazo fracionário (ex.: 1,5 mês) diretamente.

## Decisão

- Todo o cálculo interno usa `BigDecimal` com `MathContext(20,
  RoundingMode.HALF_EVEN)` (`MathConstants.PRICING_CONTEXT`). HALF_EVEN é o
  arredondamento bancário padrão (evita viés sistemático em arredondamentos
  repetidos).
- Para o expoente fracionário, usamos a biblioteca
  [big-math](https://github.com/eobermuhlner/big-math) (`BigDecimalMath.pow`),
  que implementa `x^y` para `y` não-inteiro via `exp(y · ln(x))` com precisão
  arbitrária controlada por `MathContext` — sem passar por `double` em nenhum
  momento. Encapsulado em `domain.math.FractionalPower` para isolar a
  dependência externa do resto do domínio.
- O corte para a escala de persistência (`NUMERIC(19,6)`,
  `MathConstants.MONEY_SCALE`) só acontece na borda (resposta HTTP /
  persistência), nunca nos passos intermediários do cálculo — evita
  acumulação de erro de arredondamento em cálculos encadeados.

## Consequências

- Resultado determinístico e auditável: o mesmo input sempre produz o mesmo
  output, independente de plataforma/JVM.
- Uma dependência externa a mais (`big-math`) — aceitável dado que é uma
  biblioteca pequena, madura e focada exatamente nesse problema; escrever a
  exponenciação fracionária "na mão" seria reinventar uma roda com alto risco
  de bug sutil.
