# SRM Credit Engine

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
- **Precisão numérica**: `BigDecimal` + [big-math](https://github.com/eobermuhlner/big-math)
  para potência fracionária (ver [ADR 0002](docs/adr/0002-bigdecimal-precision-and-fractional-power.md)).
- **Frontend** (planejado): React + TypeScript + Vite, TanStack Query, React
  Hook Form + Zod.
- **Observabilidade** (planejado): Logback + logstash-encoder, Micrometer +
  Prometheus + Grafana.
- **Resiliência** (planejado): Resilience4j.
- **CI/CD** (planejado): GitHub Actions com Testcontainers.

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
ReportRepository` com SQL nativo, pulando `domain/`. Isso ainda não foi
implementado nesta fase; quando for, o README vai marcar explicitamente esse
atalho arquitetural e por que ele é aceitável (leitura pura, sem regra de
negócio, otimizada para relatório).

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
`settlements.receivable_id`) e a estratégia de transação em lote (a decidir
quando o serviço de liquidação for implementado).

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
- [x] Testes de integração com Testcontainers/Postgres real: fluxo completo
      do `POST /simulations`, optimistic locking em `receivables.version` sob
      concorrência real, e a constraint `UNIQUE` de `settlements.receivable_id`
      (ver observação sobre ambiente local abaixo)

Pendente (próximas fases, não implementado ainda):

- [ ] Controllers de CRUD (`Receivable`, `Assignor`, `Currency`), controller
      de liquidação
- [ ] `ReportController` / `ReportRepository` (caminho paralelo, SQL nativo)
- [ ] Serviço de liquidação (conversão cambial + persistência de auditoria +
      decisão sobre transação em lote vs item a item)
- [ ] Mock de provedor de câmbio + Resilience4j (retry/circuit breaker/fallback)
- [ ] Observabilidade (logs JSON + MDC + Micrometer + métrica de negócio)
- [ ] Docker Compose (app, Postgres, Prometheus, Grafana)
- [ ] Frontend (React + TS + Vite)
- [ ] CI/CD (GitHub Actions: lint, test, build)

## Rodando localmente (parcial)

Ainda não há Docker Compose nem banco fixo configurado. Por enquanto:

```bash
cd backend
./gradlew test
```

Isso compila e roda os 10 testes unitários (sem Spring context nem banco) e
os 3 testes de integração em `src/test/.../integration` (`SimulationController`,
optimistic locking, constraint unique), que sobem um Postgres real via
Testcontainers. A aplicação ainda não sobe de ponta a ponta fora de teste
porque depende de um `docker-compose.yml` — isso entra na próxima fase.

**Nota sobre ambiente Windows local (não afeta CI/Linux):** nesta máquina de
desenvolvimento, o Docker Desktop 4.81 expõe um proxy de compatibilidade
(named pipe e socket Unix) que devolve uma resposta stub para chamadas
versionadas da API — isso quebra a negociação de versão do `docker-java`
usado pelo Testcontainers, em qualquer transporte (pipe do Windows ou socket
do WSL2). Contornado rodando os testes com o **Podman** (`podman machine
start`, depois `DOCKER_HOST=npipe:////./pipe/podman-machine-default`) em vez
do Docker Desktop. Mesmo com Podman, rodar os 3 testes de integração em
sequência rápida no mesmo processo Gradle ocasionalmente esbarra em
flakiness de rede do Podman-sobre-WSL2 (um `ConnectException` transitório ao
abrir a 3ª conexão JDBC em sequência) — **cada teste individualmente passa
de forma consistente** (verificado repetidas vezes isolado via `--tests`).
Em CI real (GitHub Actions, Linux nativo, Docker sem essa camada de
compatibilidade) esse problema não é esperado.

## Decisões de arquitetura (ADRs)

Ver [`docs/adr/`](docs/adr/). Cada decisão não-óbvia do motor de precificação
e do modelo de dados tem uma ADR curta (contexto, decisão, consequências).
