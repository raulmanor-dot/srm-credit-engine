# Critérios de Aceite

Requisito não funcional 5.2 do enunciado: critérios planejados que guiaram a construção e
que definem quando cada área do sistema é considerada aceita. Cada critério aponta para a
evidência concreta no repositório — nenhum é aspiracional.

## Usabilidade

| Critério | Evidência |
|---|---|
| O operador vê o valor líquido recalculado em tempo real ao ajustar taxa/data, sem clicar em "calcular" | Painel do Operador com debounce de 400ms chamando `POST /simulations` (`OperatorPanelPage.tsx`) |
| Nenhuma ação destrutiva ou de escrita acontece sem feedback visual | Notificações de sucesso/erro em toda mutação (liquidar, criar recebível/cedente) |
| Falha de comunicação com a API é distinguível de "não há dados" | Alertas de erro dedicados no Painel e na Grid (PR #27); estado vazio só aparece sem erro |
| URL inválida não resulta em tela em branco | Página 404 própria com navegação preservada + ErrorBoundary global (PR #25) |
| A raiz da API orienta quem chega sem contexto | `GET /` redireciona para o Swagger UI |

## Segurança

| Critério | Evidência |
|---|---|
| Nenhuma resposta HTTP vaza stack trace ou detalhe interno | `GlobalExceptionHandler` com catch-all sanitizado (500 genérico; detalhe só no log estruturado) |
| Input validado na borda antes de chegar ao domínio | Bean Validation nos DTOs (`@Valid`), erros de validação → 400 com mensagem por campo |
| Estados de negócio inválidos não são atingíveis via API | Dupla liquidação → 409 (optimistic lock + `UNIQUE`); simulação de título vencido → 422; transições de status validadas na entidade |
| Dados históricos de câmbio são imutáveis mesmo contra bug de aplicação | Trigger de banco impede `UPDATE`/`DELETE` em `exchange_rates` (ADR 0003) |
| Nenhuma credencial hardcoded no repositório | Configuração via env vars (`.env.example` documenta os defaults locais); verificado por varredura na auditoria final |
| CORS restrito à origem do frontend | `WebConfig` — só a origem configurada, não `*` |

## Desempenho

| Critério | Evidência |
|---|---|
| O extrato de liquidação não paga o custo do ORM | SQL nativo via `NamedParameterJdbcTemplate`, projeção direta sem entidade JPA (item 3.5, diferencial) |
| Consultas do extrato usam índices | Índices em `settled_at`, `payment_currency_id`, `assignor_id` (migrations V5/V6) |
| Paginação é server-side, nunca "traz tudo e filtra no cliente" | `LIMIT/OFFSET` no SQL; page size máximo de 200 imposto no repositório |
| A liquidação não trava esperando o provedor de câmbio | Timeouts curtos (500ms/2s) + retry + circuit breaker + fallback para taxa em banco (ADR 0006) |
| Suíte de testes rápida o suficiente para rodar a cada push | `unitTest` (sem Docker) no hook de pre-push em segundos; suíte completa ~30s local |

## Escalabilidade

| Critério | Evidência |
|---|---|
| A API não guarda estado de sessão — pronta para réplicas horizontais | Nenhuma sessão HTTP; estado só no Postgres |
| Concorrência de liquidação é segura sem lock pessimista | Optimistic lock (`@Version`) + constraint física, testado sob corrida real (Testcontainers, ADR 0004) |
| O caminho analítico já está separado do transacional | `ReportController → SettlementReportRepository` isolado — é o ponto de corte natural para read replicas ([high-scale-design.md](./high-scale-design.md)) |
| Evolução para alta escala está desenhada, não improvisada | [Design 1M tx/min](./high-scale-design.md) (cache, sharding, idempotência) e [proposta EDA](./eda-proposal.md) (outbox, eventos) |
| O sistema é observável o suficiente para escalar com dados, não com achismo | Métricas de negócio + técnicas no Prometheus, dashboard Grafana provisionado (ADR 0007) |
