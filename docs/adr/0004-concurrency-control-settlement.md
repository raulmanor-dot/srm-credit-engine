# ADR 0004 — Controle de concorrência na liquidação

## Contexto

Dois requests concorrentes não podem liquidar o mesmo recebível duas vezes
(double-spending de crédito), e uma liquidação em lote precisa de uma
política clara de o que acontece se um item do lote falhar.

## Decisão (parte implementada nesta fase)

Defesa em duas camadas independentes:

1. **Optimistic locking** — `receivables.version` com `@Version` do JPA.
   Uma tentativa de liquidar um recebível que já mudou de estado (por outra
   transação concorrente) recebe `OptimisticLockException` em vez de
   sobrescrever silenciosamente.
2. **Constraint de unicidade** — `settlements.receivable_id` é `UNIQUE`
   (`V6`). Mesmo que o optimistic lock falhe em pegar uma corrida (janela
   entre check e commit), o banco rejeita fisicamente uma segunda linha de
   liquidação para o mesmo recebível.

`settlements` também tem seu próprio `version` (`@Version`), preparando o
terreno para eventuais correções/estornos controlados no futuro.

## Decisão pendente (a resolver quando o serviço de liquidação em lote for
implementado)

Duas opções para o lote, ainda em aberto:

- **Tudo ou nada**: uma única `@Transactional` para o lote inteiro; qualquer
  item que falhe (recebível já liquidado, tipo sem Strategy, etc.) faz
  rollback de todo o lote.
- **Item a item com status individual**: cada item processado em sua própria
  transação; o lote retorna um relatório por item (sucesso/falha), sem
  reverter os itens que já passaram.

Este README/ADR será atualizado com a escolha e a justificativa assim que o
serviço de liquidação for implementado — registrar aqui a intenção evita que
a decisão pareça acidental quando o código chegar.

## Consequências

- Duas camadas de proteção significam que um bug em uma não compromete a
  integridade — é defesa em profundidade, não redundância inútil.
- A decisão pendente do lote afeta diretamente a experiência do operador
  (tudo-ou-nada é mais simples de raciocinar, mas pode barrar 99 liquidações
  válidas por causa de 1 inválida); vale decidir com esse trade-off explícito.
