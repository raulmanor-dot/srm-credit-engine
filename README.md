# SRM Credit Engine

[![CI](https://github.com/raulmanor-dot/srm-credit-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/raulmanor-dot/srm-credit-engine/actions/workflows/ci.yml)

Plataforma de cessão de crédito multimoedas (FIDC): cadastro de recebíveis,
precificação (valor presente) e liquidação, com auditabilidade de todas as
taxas aplicadas.

Este README é vivo — atualizado a cada fase de implementação. Ele documenta
não só o que existe, mas as decisões técnicas por trás, porque é isso que se
espera de nível Sênior nesta avaliação.

## Escopo do desafio (referência)

- Cadastro de cedentes, recebíveis e tipos de recebível (com spread
  configurável em banco).
- Motor de precificação: valor presente de um recebível a partir de uma taxa
  base + spread do tipo, com juros compostos e prazo fracionário em meses.
- Liquidação de recebíveis com conversão cambial e persistência de auditoria
  (snapshot das taxas usadas).
- Extrato de liquidação com um caminho paralelo `ReportController →
  ReportRepository` (SQL nativo), sem passar pela camada de negócio.
- Observabilidade (logs estruturados + métricas), resiliência (retry/circuit
  breaker no provedor de câmbio), CI/CD, frontend em React.

## Stack

- **Backend**: Java 21, Spring Boot 3.3, Gradle (wrapper), PostgreSQL, Flyway.
- **Containerização**: Docker multi-stage (`backend/Dockerfile`) + Docker
  Compose (app + Postgres, healthchecks).
- **Precisão numérica**: `BigDecimal` + [big-math](https://github.com/eobermuhlner/big-math)
  para potência fracionária (ver [ADR 0002](docs/adr/0002-bigdecimal-precision-and-fractional-power.md)).
- **Frontend**: React + TypeScript + Vite, Mantine (UI + `@mantine/form`),
  TanStack Query, React Router.
- **Observabilidade** (planejado): Logback + logstash-encoder, Micrometer +
  Prometheus + Grafana.
- **Resiliência** (planejado): Resilience4j.
- **CI/CD**: GitHub Actions (`.github/workflows/ci.yml`) — job `backend`
  (`./gradlew test` com Testcontainers) + job `frontend` (lint + build), ver
  [CI/CD](#cicd).

## Arquitetura

Camadas dentro de `backend/src/main/java/com/srmasset/creditengine`:

```
application/   controllers, DTOs, exception handling HTTP
domain/        regras de negócio puras (pricing, exceptions, services)
persistence/   entidades JPA e repositórios Spring Data
```

O motor de precificação (`domain/pricing`) não depende de Spring MVC nem de
JPA além das entidades — é testável isoladamente, sem contexto Spring nem
banco (ver testes em `domain/pricing/*Test.java`, quando existirem).

O **extrato de liquidação** é o caso deliberado que quebra essa regra: o
enunciado permite (item 3.6) um caminho paralelo `ReportController →
SettlementReportRepository`, com SQL nativo (`NamedParameterJdbcTemplate`),
pulando `SettlementService`, `PricingStrategy` e as entidades JPA por
completo — `GET /reports/settlements` é leitura pura para relatório, sem
invariante de negócio a proteger, então o atalho é aceitável. O repositório
retorna um `record` de projeção (`persistence.report.SettlementStatementRow`)
diretamente na resposta HTTP, sem remapear para um DTO — remapear
esconderia o próprio ponto que esse endpoint demonstra.

## Motor de precificação

`PricingStrategy` é a interface (`calculatePresentValue(Receivable,
BaseRate)`), resolvida por tipo via `PricingStrategyResolver` — um mapa
`ReceivableTypeCode -> PricingStrategy` construído a partir de todos os beans
`PricingStrategy` que o Spring injeta. Adicionar um novo tipo de recebível não
exige tocar no resolver nem em if/else: basta uma nova classe `@Component`.
Isso é Open/Closed na prática.

`DuplicataMercantilStrategy` e `ChequePreDatadoStrategy` estendem
`AbstractCompoundInterestPricingStrategy`, que implementa a fórmula de juros
compostos comum às duas. O spread mensal (1,5% / 2,5%) **não é constante
Java** — vem de `receivable_types.spread_percent_monthly` (seed em
`V7__seed_reference_data.sql`), ajustável em banco sem deploy.

Decisões que evitam o erro clássico de ambiguidade em taxa mensal:

- [ADR 0001](docs/adr/0001-day-count-convention.md) — convenção de contagem de prazo.
- [ADR 0002](docs/adr/0002-bigdecimal-precision-and-fractional-power.md) — precisão numérica e potência fracionária.

## Cadastros (CRUD)

`CurrencyController`, `AssignorController` e `ReceivableController` expõem
CRUD para `Currency`, `Assignor` e `Receivable`. Cada um segue o mesmo padrão
em camadas dos demais endpoints (`Controller → Service → Repository`),
diferente do atalho deliberado do `ReportController`.

- **"Excluir" nunca é `DELETE FROM`**: `Currency` e `Assignor` têm coluna
  `active` desde o schema original (V1/V3) — `DELETE /currencies/{id}` e
  `DELETE /assignors/{id}` apenas desativam (`active = false`), porque ambos
  podem estar referenciados por FK em `receivables`/`exchange_rates`/
  `settlements`; um hard delete quebraria a integridade referencial ou, pior,
  apagaria histórico de auditoria. Há também `POST /{id}/activate` para
  reverter.
- **`Receivable` não tem coluna `active`, tem `status`**: "excluir" um
  recebível é uma transição de estado (`markAsCanceled()`), não uma remoção
  física — `DELETE /receivables/{id}` chama esse método, que (como
  `markAsSettled()`) só é permitido a partir de `PENDING` e devolve `409` via
  `ReceivableNotPendingException` caso contrário. Um recebível já `SETTLED`
  tem um `Settlement` de auditoria vinculado; cancelar por aqui retroagiria
  sobre esse snapshot, então é bloqueado.
- **`PUT /receivables/{id}` (`Receivable.amend`)** só altera `faceValue`,
  `documentNumber` e as datas, e também exige `status == PENDING` pelo mesmo
  motivo. `assignorId`/`receivableTypeId`/`faceValueCurrencyId` são a
  identidade do recebível e não são editáveis após criado — trocar o cedente
  ou o tipo de um recebível já cadastrado não é uma "edição", é outro
  recebível.
- **Código de moeda (`Currency.code`) e CPF/CNPJ (`Assignor.taxId`) são
  imutáveis** após criados — só o nome pode ser atualizado
  (`CurrencyUpdateRequest`/`AssignorUpdateRequest` não têm esses campos).
  São a chave de negócio; mudar o valor seria trocar de entidade, não
  corrigir um cadastro.
- Conflitos (código/CPF duplicado, editar/cancelar um recebível fora de
  `PENDING`) chegam ao `GlobalExceptionHandler` já existente e viram `409`
  sem handler novo: `DataIntegrityViolationException` (constraint `UNIQUE`)
  e `ReceivableNotPendingException` já eram tratados antes desta fase.

## Frontend

`frontend/` (Vite + React + TypeScript). Escopo desta primeira fase segue
literalmente o item 4 do enunciado ("Escopo Técnico — Frontend"), não o
CRUD completo do backend:

- **Painel do Operador** (`/`, `OperatorPanelPage`): o operador seleciona um
  recebível `PENDING` (`GET /receivables?status=PENDING`) e ajusta a taxa
  base / data de referência; o valor líquido é recalculado **em tempo real**
  chamando `POST /simulations` com debounce (400ms, `@mantine/hooks`
  `useDebouncedValue`). O cálculo em si nunca é replicado em TS — o
  comentário original do `SimulationController` já previa exatamente este
  uso ("o painel de simulação chama este endpoint com debounce").
- **Grid de Transações** (`/transacoes`, `TransactionsGridPage`): histórico
  de liquidações via `GET /reports/settlements`, com **paginação
  server-side** (`page`/`size`) e filtros dinâmicos (data de/até, cedente).
  O backend não expõe total de páginas (decisão deliberada, ver
  `SettlementReportRepository`) — "próxima página" é habilitada
  heuristicamente enquanto a página atual vier cheia (`size` itens).
- **Ações no Painel do Operador** (fase seguinte, para o painel deixar de
  ser só leitura): botão **"+ Novo recebível"** abre um modal
  (`NewReceivableModal`) que cria um recebível via `POST /receivables`,
  com criação rápida de cedente embutida (`POST /assignors` inline, sem
  sair do formulário) — sem isso, popular o painel dependia de `curl`
  direto na API. Depois de simular, botão **"Liquidar"** chama `POST
  /settlements` com os mesmos parâmetros já usados na simulação (mesma
  conta, agora persistida) e sugere a moeda de pagamento igual à moeda de
  face do recebível (editável, para exercitar a conversão cambial). Ambas
  as ações invalidam as queries afetadas (TanStack Query) — o recebível
  liquidado some da lista de pendentes e aparece na Grid de Transações
  sem precisar recarregar a página. Endpoint novo de apoio: `GET
  /receivable-types` (`ReceivableTypeController`), só leitura, sem
  `Service` — é dado de referência estático usado apenas para popular o
  seletor "Tipo" do formulário, sem regra de negócio a encapsular.
- **Arquitetura**: `src/api/*` isola toda chamada HTTP e cache
  (TanStack Query) em hooks (`useReceivables`, `useSimulation`,
  `useSettlementReport`...); `src/pages/*` só consome esses hooks e
  renderiza — nenhuma lógica de negócio ou fetch inline em componente de
  UI. Estado global de servidor fica no `QueryClient` (cache/refetch);
  não há Redux/Zustand porque não há estado de UI compartilhado entre
  páginas que justifique isso.
- **UI**: Mantine (`@mantine/core`, `@mantine/dates`, `@mantine/form`) —
  biblioteca de componentes pronta para reduzir tempo de setup, conforme
  decisão explícita nesta fase (troca do React Hook Form + Zod
  originalmente planejado, redundante com `@mantine/form`).
- CRUD de cadastros (moedas/cedentes/recebíveis) já existe na API (ver
  [Cadastros (CRUD)](#cadastros-crud)) mas não tem tela própria ainda —
  ficou fora do escopo desta fase por não estar no item 4 do enunciado.

### Rodando o frontend

```bash
cd frontend
cp .env.example .env   # VITE_API_BASE_URL, default http://localhost:8080
npm install
npm run dev            # http://localhost:5173
```

Requer a API rodando (`docker compose up` na raiz — ver
[Rodando localmente](#rodando-localmente)). CORS é liberado no backend só
para a origem do frontend (`app.cors.allowed-origin`, default
`http://localhost:5173`, ver `WebConfig`).

## Modelo de dados

Migrations Flyway em `backend/src/main/resources/db/migration`:

| Migration | Tabela | Observação |
|---|---|---|
| V1 | `currencies` | catálogo de moedas |
| V2 | `receivable_types` | spread configurável em banco |
| V3 | `assignors` | cedentes |
| V4 | `exchange_rates` | série temporal **append-only** — ver [ADR 0003](docs/adr/0003-exchange-rates-append-only.md) |
| V5 | `receivables` | `version` (optimistic lock) + `status` |
| V6 | `settlements` | auditoria completa: taxa base, spread, câmbio, valores em ambas moedas |
| V7 | seed | moedas BRL/USD, os dois tipos de recebível com seus spreads, 1 câmbio inicial |

Todos os valores monetários são `NUMERIC(19,6)`.

Concorrência na liquidação: [ADR 0004](docs/adr/0004-concurrency-control-settlement.md)
descreve a defesa em duas camadas (`@Version` em `receivables` + `UNIQUE` em
`settlements.receivable_id`) e a estratégia de lote item a item, incluindo o
mecanismo de `@Transactional(REQUIRES_NEW)` em bean separado que evita a
armadilha clássica de auto-invocação em proxies do Spring.

## Status atual

Implementado neste commit:

- [x] Scaffold Gradle (wrapper, Spring Boot 3.3, Java 21 toolchain)
- [x] Migrations V1–V7 (schema completo + seed)
- [x] Entidades JPA (`Currency`, `ExchangeRate`, `ReceivableType`, `Assignor`,
      `Receivable` com `@Version`, `Settlement` com `@Version`)
- [x] Repositórios Spring Data para todas as entidades
- [x] Motor de precificação completo (`PricingStrategy`, as duas
      implementações, `PricingStrategyResolver`, `TermCalculator`,
      `FractionalPower`)
- [x] Camada de aplicação mínima: `SimulationController` (`POST
      /simulations`), `SimulationService`, `GlobalExceptionHandler`
- [x] Testes unitários do motor de precificação (10 testes, sem Spring
      context nem banco — `TermCalculator`, as duas Strategies e o
      `PricingStrategyResolver`)
- [x] Serviço de liquidação (`SettlementService`): calcula valor presente,
      converte para a moeda de pagamento quando necessário (via
      `ExchangeRateService`, com lookup bidirecional — funciona mesmo quando
      só existe a taxa no sentido inverso ao seed) e persiste o `Settlement`
      como snapshot de auditoria completo
- [x] Liquidação em lote item a item (`SettlementBatchService`) — decisão e
      mecanismo de transação (`REQUIRES_NEW` em bean separado) documentados
      na [ADR 0004](docs/adr/0004-concurrency-control-settlement.md)
- [x] `POST /settlements` e `POST /settlements/batch`
- [x] `ReportController` / `SettlementReportRepository`: caminho paralelo com
      SQL nativo (`NamedParameterJdbcTemplate`), pulando `SettlementService`
      e as entidades JPA — exatamente o atalho permitido pelo item 3.6
- [x] Testes de integração com Testcontainers/Postgres real: fluxo completo
      do `POST /simulations`, optimistic locking em `receivables.version` sob
      concorrência real, a constraint `UNIQUE` de `settlements.receivable_id`,
      liquidação em lote com falha parcial, e o relatório
      (ver observação sobre ambiente local abaixo)

- [x] Docker Compose (app + Postgres) — ver [Rodando localmente](#rodando-localmente)
- [x] Controllers de CRUD (`Receivable`, `Assignor`, `Currency`) — ver
      [Cadastros (CRUD)](#cadastros-crud): soft delete em `Currency`/
      `Assignor`, cancelamento por transição de estado em `Receivable`
- [x] Frontend (React + TS + Vite + Mantine) — ver [Frontend](#frontend):
      Painel do Operador (simulação em tempo real com debounce) e Grid de
      Transações (extrato paginado server-side, filtros dinâmicos),
      verificado ponta a ponta no navegador (Playwright headless)
- [x] Ações no Painel do Operador: criar recebível (com criação rápida de
      cedente inline) e liquidar direto do painel, sem precisar de `curl` —
      fluxo completo (criar cedente → criar recebível → simular → liquidar
      → aparece na grid) verificado no navegador
- [x] CI/CD (GitHub Actions) — ver [CI/CD](#cicd): job `backend`
      (`./gradlew test`, suíte completa) + job `frontend` (lint + typecheck
      + build), disparados em push/PR contra `main`

Pendente (próximas fases, não implementado ainda):

- [ ] Mock de provedor de câmbio + Resilience4j (retry/circuit breaker/fallback)
- [ ] Observabilidade (logs JSON + MDC + Micrometer + métrica de negócio,
      Prometheus/Grafana no compose)
- [ ] Telas de cadastro (CRUD) no frontend — API já existe, sem UI própria
      ainda (fora do escopo do item 4 do enunciado)
- [ ] Lint/formatter para o backend (Checkstyle ou Spotless) — não existe
      ainda, ver [CI/CD](#cicd)

## Rodando localmente

### Configuração inicial (uma vez, após clonar)

```bash
git config core.hooksPath .githooks
```

Ativa os git hooks do projeto (`commit-msg` valida Conventional Commits,
`pre-push` roda os testes unitários) — ver [Estratégia de Git](#estratégia-de-git).

### Via Docker Compose (aplicação completa)

Requer apenas Docker. Sobe Postgres e a aplicação (build multi-stage do
`backend/Dockerfile`, sem precisar de Java/Gradle instalados na máquina):

```bash
docker compose up --build
```

A API fica disponível em `http://localhost:8080` (ex.: `POST
/simulations`) e o Postgres em `localhost:5432` (`srm`/`srm`,
banco `srm_credit_engine` — sobrescrevível via `.env`, ver `.env.example`).
As migrations Flyway rodam automaticamente no boot. Healthcheck de cada
serviço:

- `postgres`: `pg_isready`.
- `app`: `GET /actuator/health` (exposto por `spring-boot-starter-actuator`,
  já presente no `build.gradle`).

`app` só inicia depois que `postgres` reporta `healthy`
(`depends_on: condition: service_healthy`). Para derrubar tudo e limpar o
volume de dados: `docker compose down -v`.

### Testes (sem subir o compose)

```bash
cd backend
./gradlew test
```

Isso compila e roda os 19 testes unitários (sem Spring context nem banco) e
os 18 testes de integração em `src/test/.../integration` (9 classes:
CRUD de `Assignor`/`Currency`/`Receivable`, optimistic locking, constraint
unique, extrato/relatório, simulação e liquidação), que sobem um Postgres
real via Testcontainers — mecanismo independente do `docker-compose.yml`
acima.

`AbstractIntegrationTest` usa o padrão "singleton container" do
Testcontainers: um único Postgres é iniciado uma vez por JVM num bloco
`static` (não via `@Testcontainers`/`@Container`, cuja extensão do JUnit5
trata o campo como escopo *por classe* — inicia antes e para depois de
cada classe que o usa, mesmo sendo um campo estático herdado — o que
derrubava e recriava o container no meio da suíte enquanto o Spring
reaproveitava o `ApplicationContext` cacheado da classe anterior, ainda
apontando para a porta antiga; foi exatamente isso, e não uma
particularidade do Windows/Podman, que causava o `ConnectException`
esporádico observado antes desta fase). Como o banco agora é
genuinamente compartilhado por toda a suíte, os dados de fixture
(CPF/CNPJ de `Assignor` nos testes) precisam ser únicos entre classes —
colisões de `UNIQUE` apareceram e foram corrigidas ao mudar para essa
suíte de testes verdadeiramente compartilhada.

**Nota sobre ambiente Windows local (afeta só Testcontainers, não o
`docker compose up` acima nem CI/Linux):** nesta máquina de
desenvolvimento, o Docker Desktop 4.81 expõe um proxy de compatibilidade
(named pipe e socket Unix) que devolve uma resposta stub para chamadas
versionadas da API — isso quebra a negociação de versão do `docker-java`
usado pelo Testcontainers, em qualquer transporte (pipe do Windows ou socket
do WSL2). Contornado rodando os testes com o **Podman** (`podman machine
start`, depois `DOCKER_HOST=npipe:////./pipe/podman-machine-default`) em vez
do Docker Desktop.

## CI/CD

`.github/workflows/ci.yml`, dois jobs independentes, disparados em push
para `main` e em todo Pull Request contra `main`:

- **`backend`**: `./gradlew test` — suíte completa (unitários +
  integração via Testcontainers) em `ubuntu-latest`. Ao contrário desta
  máquina de desenvolvimento Windows (ver observação acima sobre
  Podman/WSL2), o runner tem Docker nativo — a flakiness observada
  localmente é especificamente da camada de compatibilidade do Docker
  Desktop no Windows, não esperada em CI. Relatórios de teste sobem como
  artefato (`actions/upload-artifact`, 7 dias de retenção) mesmo quando o
  job falha (`if: always()`), para inspecionar sem precisar reproduzir
  localmente.
- **`frontend`**: `npm run lint` (oxlint) + `npm run build` (`tsc -b` +
  `vite build`) em `ubuntu-latest`. Falha de typecheck ou de lint quebra
  o build, não só o `vite build` em si.
- Sem lint configurado para o backend (sem Checkstyle/Spotless no
  `build.gradle`) — adicionar um agora reformataria todo o código
  existente num PR que deveria ser só infraestrutura de CI; fica como
  débito técnico registrado, não decisão silenciosa.
- Cache: `actions/setup-java` (`cache: gradle`) e `actions/setup-node`
  (`cache: npm`, `cache-dependency-path: frontend/package-lock.json`) —
  builds subsequentes reaproveitam dependências já baixadas.
- Nenhum job de deploy: o enunciado pede o pipeline (lint/test/build),
  não hospedagem — não há ambiente de produção para publicar.

## Estratégia de Git

Fluxo adotado: **GitHub Flow** (branches curtas de feature a partir da
`main`, PR, merge, deploy contínuo da `main`) — não Git Flow, porque não há
múltiplas versões em produção simultâneas nem necessidade de branches
`develop`/`release` para esse projeto; a complexidade extra do Git Flow não
se paga aqui.

- **Uma branch por feature**, nomeada `feature/<assunto>` (ex.:
  `feature/settlement-service`), nunca commit direto na `main`.
- **Conventional Commits** obrigatório (`feat:`, `fix:`, `test:`, `docs:`,
  `chore:`, `refactor:`...), reforçado por um git hook (`commit-msg`) que
  rejeita mensagens fora do padrão.
- **Pull Request por feature**, mesmo trabalhando sozinho — a descrição da
  PR documenta o que foi feito (ver [PRs #1–#5](../../pulls?q=is%3Apr) no
  repositório).
- **Merge via rebase, não merge commit**: cada branch de feature tem
  commits organizados (squash de fixups já feito antes do PR) e é
  integrada à `main` com `git rebase`/"Rebase and merge" — resultado é um
  fast-forward, então a `main` fica **100% linear, sem bolhas de merge**.
  Isso é o que a avaliação chama de "Histórico Limpo" (Pleno) e
  "Interactive Rebase... mantendo uma linearidade profissional" (Sênior):
  aqui não é só teoria, é a estratégia de merge configurada de verdade no
  repositório.
- **Git hooks** (`.githooks/`, versionados — equivalente ao que o Husky faz
  para projetos Node, mas nativo do Git): rode `git config core.hooksPath
  .githooks` uma vez após clonar (setup manual porque Git não versiona
  hooks nem os instala sozinho a partir do clone).
  - `commit-msg`: valida Conventional Commits antes de aceitar o commit.
  - `pre-push`: roda `./gradlew unitTest` (subconjunto rápido, sem
    Testcontainers/Docker — ver `backend/build.gradle`) antes de qualquer
    `git push`, para nunca empurrar um commit que quebra os testes
    unitários.
- **Tags SemVer** marcando marcos de entrega (`git tag -l`):
  - `v0.1.0` — motor de precificação + endpoint de simulação testado.
  - `v0.2.0` — liquidação completa (individual, lote, extrato).
  - `v0.3.0` — dockerizado (Docker Compose local).
  - `v1.0.0` será a tag da entrega final, quando observabilidade,
    resiliência, frontend e CI/CD estiverem completos.

## Decisões de arquitetura (ADRs)

Ver [`docs/adr/`](docs/adr/). Cada decisão não-óbvia do motor de precificação
e do modelo de dados tem uma ADR curta (contexto, decisão, consequências).
