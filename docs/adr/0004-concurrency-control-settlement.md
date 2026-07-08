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

## Decisão: liquidação em lote item a item

Escolhido **item a item, com status individual** em vez de tudo-ou-nada: um
recebível problemático (já liquidado, optimistic lock, câmbio ausente) não
pode bloquear a liquidação dos demais itens do lote — operacionalmente, isso
seria inaceitável (99 liquidações válidas represadas por 1 inválida).

Implementação (`SettlementService` + `SettlementBatchService`):

- `SettlementService.settle(...)` é `@Transactional(propagation =
  Propagation.REQUIRES_NEW)` — cada liquidação é sua própria unidade
  atômica, sempre, independente de quem a chama.
- `SettlementBatchService` injeta `SettlementService` como **bean separado**
  e itera a lista chamando `settlementService.settle(...)` dentro de um
  `try/catch` por item, agregando um resultado (sucesso ou mensagem de erro)
  por `receivableId`.
- O motivo do bean separado: se o laço e o método transacional estivessem
  na mesma classe, uma chamada `this.settle(...)` não passaria pelo proxy
  do Spring (auto-invocação não é interceptada por proxies AOP), e o
  `REQUIRES_NEW` nunca seria acionado — o item silenciosamente rodaria
  dentro da transação (inexistente) do chamador. Injetar como colaborador
  garante que cada chamada é uma chamada externa de verdade ao proxy.
- `SettlementBatchService.settleBatch(...)` **não** é `@Transactional`: uma
  transação ali não mudaria o isolamento por item (`REQUIRES_NEW` sempre
  suspende qualquer transação em curso), só manteria uma conexão aberta
  pela duração inteira do lote sem necessidade.

Trade-off aceito: para lotes muito grandes, isso abre/fecha mais conexões
ao longo do tempo do que uma única transação grande — inerente à escolha de
commits independentes por item, não um problema a esta escala.

## Consequências

- Duas camadas de proteção significam que um bug em uma não compromete a
  integridade — é defesa em profundidade, não redundância inútil.
- A decisão pendente do lote afeta diretamente a experiência do operador
  (tudo-ou-nada é mais simples de raciocinar, mas pode barrar 99 liquidações
  válidas por causa de 1 inválida); vale decidir com esse trade-off explícito.
