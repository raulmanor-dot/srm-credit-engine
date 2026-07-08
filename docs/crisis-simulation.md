# Simulação de gestão de crise: bug crítico de precificação em produção

Entregável de nível Especialista/Staff (item 6 do enunciado): reencenação controlada de um
incidente real de engenharia — um bug que passa pela revisão e pelo CI, chega à `main`, e
precisa ser revertido com segurança. Commits reais no histórico deste repositório: PR #22
(o bug) e o commit de revert que o segue.

## O que aconteceu

A PR "fix: correct spread sign in compound interest pricing formula" (#22) alterou
`AbstractCompoundInterestPricingStrategy` para **subtrair** o spread de risco da taxa base,
em vez de somá-lo — invertendo o sinal da fórmula documentada no
[ADR 0005](./adr/0005-pricing-strategy-open-closed.md) e no README. A justificativa do
autor da mudança, registrada no próprio commit, foi que somar o spread "parecia" contar o
risco em dobro sobre uma taxa base que já seria ajustada a risco — um raciocínio plausível
à primeira leitura, mas que inverte o propósito do spread: ele existe para **aumentar** o
desconto sobre títulos mais arriscados (cheque pré-datado tem spread maior que duplicata,
propositalmente), não para reduzi-lo.

**Por que passou pela revisão e pelo CI:** os testes unitários que validam a fórmula
(`ChequePreDatadoStrategyTest`, `DuplicataMercantilStrategyTest`) e o teste de integração
de simulação foram atualizados **na mesma PR**, com os valores esperados recalculados para
bater com a fórmula errada. A suíte ficou verde porque o teste deixou de validar a fórmula
correta — validava a nova fórmula, consistentemente errada. Esse é o padrão mais comum de
bug crítico que atravessa CI: não falta teste, o teste foi ajustado para concordar com o
código errado.

## Como foi detectado

Não por teste automatizado — por validação manual de sanidade financeira depois do deploy.
Simulando um recebível do tipo `CHEQUE_PRE_DATADO` (spread de 2,5% a.m.) com uma taxa base
de mercado realista de 1,0% a.m.:

```
POST /simulations {"receivableId": 36, "baseRateMonthlyPercent": 1.0}
→ faceValue: 217321.05
→ presentValue: 221856.56   ← MAIOR que o valor de face
```

Um valor presente **maior** que o valor de face é, por definição, financeiramente absurdo
para uma operação de deságio: o fundo estaria pagando mais do que o título vale, para o
tipo de recebível com o maior spread de risco cadastrado — exatamente o oposto do que a
regra de negócio existe para proteger. Bastou reconhecer o domínio (nenhum resultado de
"valor presente com deságio" pode superar o valor de face quando a taxa total é positiva)
para identificar que a fórmula, não o dado, estava errada.

## Por que `git revert`, não `git reset`

A `main` já havia avançado (outras PRs, incluindo a de documentação Especialista/Staff)
depois do merge do bug — um `reset --hard` reescreveria histórico compartilhado e exigiria
force-push, arriscando o trabalho de qualquer PR aberta em paralelo. `git revert` cria um
novo commit que desfaz exatamente as mudanças da PR problemática, preservando o histórico
linear e sendo seguro para uma branch protegida (respeita CI e revisão como qualquer outro
commit).

```bash
git revert <sha-do-merge-commit-da-PR-22> -m 1
```

O revert restaura `.add(spreadPercent, MC)` e os três testes ao estado correto, e passa
pelo mesmo pipeline de CI que qualquer mudança normal — a correção em si não é um bypass de
processo, é o processo funcionando.

## Lição

Um "fix" que altera simultaneamente o código de produção **e** o valor esperado do teste
que deveria pegá-lo merece revisão redobrada — é precisamente o cenário em que a suíte
verde mais engana. A defesa adicional que este incidente sugere: um teste de propriedade
(`presentValue <= faceValue` sempre que a taxa total for não-negativa) pegaria essa classe
inteira de bug independentemente do valor numérico exato esperado, sem exigir que alguém
recalcule manualmente o valor "certo" toda vez que a fórmula mudar.
