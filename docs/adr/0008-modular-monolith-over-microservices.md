# ADR 0008 — Monolito modular em vez de microserviços

## Contexto

O enunciado cita "Monolito vs Microserviços" como exemplo de decisão difícil a
documentar, e a pergunta é inevitável para um sistema financeiro que ambiciona
escala (ver [high-scale-design.md](../high-scale-design.md), 1M tx/min). A
tentação de decompor cedo é real — microserviços são o padrão de mercado para
"sistema sério" — mas a decomposição tem custos permanentes e benefícios que só
se materializam sob condições específicas.

As condições que justificam microserviços: múltiplos times precisando de deploy
independente sem coordenação (Lei de Conway), domínios com ciclos de vida e
perfis de carga genuinamente distintos, ou um gargalo mensurável que a separação
resolve. Nenhuma delas existe neste sistema: um domínio coeso (cessão de
crédito), um time, um ciclo de deploy.

O fator decisivo é transacional. A liquidação — validar status, precificar,
converter câmbio, persistir o settlement, marcar o recebível — é hoje **um
único commit ACID no Postgres**, o que atende literalmente o requisito
"nenhuma liquidação pode ficar pela metade". Separar precificação, câmbio e
liquidação em serviços transformaria esse commit em uma saga com compensação:
trocaríamos a garantia mais forte do sistema por consistência eventual
exatamente no ponto onde há dinheiro.

## Decisão

Monolito modular:

- **Uma aplicação, um deploy, um banco** — a liquidação permanece uma transação
  local ACID.
- **Fronteiras internas com disciplina de serviço**: camadas
  `application`/`domain`/`persistence` sem violação de dependência (o domínio
  não importa nada da camada de aplicação), motor de precificação puro e sem
  dependência de Spring MVC. Se um módulo precisar virar serviço, a extração é
  mecânica, não uma reescrita.
- **Fronteiras de rede só onde já fazem sentido**: o provedor de câmbio é
  acessado via HTTP real com retry/circuit breaker (ADR 0006) — a fronteira
  externa já existe onde há de fato um sistema externo. O extrato analítico
  (`ReportController` → SQL nativo, fora da camada de domínio) é o ponto de
  corte natural para um serviço de leitura/read replica futuro.
- **O caminho de decomposição está documentado, não improvisado**:
  [eda-proposal.md](../eda-proposal.md) descreve como os efeitos
  pós-liquidação virariam eventos (outbox transacional) e
  [high-scale-design.md](../high-scale-design.md) descreve o sharding por
  `assignor_id` — juntos, são o desenho de como o sistema se decompõe *quando
  houver um motivo mensurável*, preservando o núcleo transacional.

## Consequências

- Ganho imediato: zero custo de rede interna, zero versionamento de contrato
  entre módulos, zero transação distribuída, observabilidade simples (um
  processo), e todo o tempo do projeto investido em corretude financeira,
  concorrência e resiliência em vez de em infraestrutura de distribuição.
- Custo aceito: escala vertical/horizontal por réplica do monolito inteiro
  (mitigado por ele ser stateless), e a disciplina de fronteira interna precisa
  ser mantida por revisão — não há um limite de rede forçando a separação.
  A verificação de dependências entre camadas na auditoria final é o controle
  disso.
- Critério de reversão explícito: a decisão muda quando existirem múltiplos
  times com necessidade de deploy independente, ou quando o perfil de carga
  analítico/transacional divergir a ponto de uma read replica não bastar.
  Decompor antes disso seria pagar o custo distribuído sem o benefício — o
  anti-padrão do "monolito distribuído" (serviços acoplados que fazem deploy
  juntos, mas com rede e falha parcial no meio) é um estado estritamente pior
  que qualquer um dos dois extremos.
