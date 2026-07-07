# ADR 0003 — `exchange_rates` como série temporal append-only

## Contexto

Auditabilidade exige saber qual taxa cambial estava vigente no momento de
cada liquidação. Se a tabela de câmbio permitisse `UPDATE`, uma "correção"
de taxa reescreveria silenciosamente o histórico, invalidando qualquer
liquidação já auditada com base naquele valor.

## Decisão

- `exchange_rates` nunca recebe `UPDATE` ou `DELETE`. Uma nova cotação é
  sempre uma nova linha (`base_currency_id`, `quote_currency_id`, `rate`,
  `valid_from`).
- A "taxa vigente" de um par de moedas é a linha mais recente por
  `valid_from` (`ExchangeRateRepository.findFirstBy...OrderByValidFromDesc`).
- Reforçado no próprio banco: um trigger (`prevent_exchange_rate_mutation`,
  em `V4`) rejeita `UPDATE`/`DELETE` com exceção, mesmo que um bug de
  aplicação tente. Defesa em profundidade — não depende só da disciplina do
  código Java.
- `settlements` grava a taxa efetivamente usada (`fx_rate_used`) como
  snapshot, então mesmo que novas cotações cheguem depois, a liquidação
  antiga permanece auditável sem precisar fazer join com `exchange_rates`.

## Consequências

- Nenhuma "correção retroativa" de taxa é possível — erros de cotação exigem
  uma nova linha corretiva, preservando o histórico completo.
- Consulta de "taxa vigente" é sempre `ORDER BY valid_from DESC LIMIT 1`, não
  uma coluna de flag mutável — mais simples de raciocinar sobre concorrência.
