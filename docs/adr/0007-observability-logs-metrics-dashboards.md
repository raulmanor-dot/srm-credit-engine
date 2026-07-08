# ADR 0007 — Logs estruturados, métricas de negócio e Prometheus/Grafana

## Contexto

Última fase pendente antes da tag `v1.0.0`. O enunciado pede observabilidade
de verdade: logs estruturados com correlação por requisição/liquidação,
métricas via Micrometer que incluam pelo menos uma métrica de negócio (não
só técnica), e Prometheus + Grafana no compose. A aplicação já expõe
`/actuator/circuitbreakers` e `/actuator/retries` (ADR 0006) — essa fase
precisa conectar essa infraestrutura a métricas de verdade, não duplicá-la.

## Decisão

- **JSON no stdout, sem log shipper.** `logback-spring.xml` usa
  `logstash-logback-encoder` para emitir JSON no console no perfil padrão
  (o que o `app` do compose já roda); um perfil `dev` opcional
  (`SPRING_PROFILES_ACTIVE=dev`) mantém um `%pattern` legível para
  desenvolvimento local. Nenhum Loki/ELK é adicionado — o compose já ganha
  dois serviços novos (Prometheus/Grafana) nesta fase, e um agregador de
  logs seria uma segunda peça de infraestrutura sem exigência do enunciado.
- **Duas chaves de MDC, dois escopos de vida**: `requestId` (uma por
  requisição HTTP, setada por `CorrelationIdFilter`, `application/config`,
  aceitando `X-Request-Id` de entrada ou gerando um UUID, sempre ecoado na
  resposta) e `receivableId` (uma por liquidação, setada/removida dentro de
  `SettlementService.settle`, em try/finally). Duas chaves porque uma
  liquidação em lote (`SettlementBatchService`, ADR 0004) processa vários
  recebíveis sequencialmente na mesma requisição/thread — `requestId`
  sozinho não distingue qual item do lote gerou qual linha de log.
  `MDC.put`/`remove` simples é suficiente porque o processamento é
  sequencial na mesma thread (Tomcat, sem `@Async`/virtual threads); não há
  necessidade de um mecanismo de propagação mais sofisticado.
- **Métricas de negócio via `@Component` injetado diretamente** no
  serviço (`SettlementMetricsRecorder` em `SettlementService`,
  `ExchangeRateMetricsRecorder` em `ExchangeRateService`), não via
  `ApplicationEvent`/listener nem AOP. Há exatamente um produtor e um ponto
  de emissão por métrica — um barramento de eventos aqui seria a mesma
  seam especulativa que a ADR 0006 já rejeitou para a persistência da
  cotação. `SettlementMetricsRecorder.recordSettlement` é chamado uma única
  vez, dentro de `settle()`, depois do `saveAndFlush` — tanto o caminho do
  controller quanto o do lote passam por esse método, então a
  instrumentação não é duplicada por chamador.
- **Métricas de negócio registradas**: `settlements.count` (Counter, tag
  `currency`) e `settlements.volume` (DistributionSummary, tag `currency`)
  — liquidações e volume líquido por moeda de pagamento; `fx.provider.requests`
  (Counter, tag `outcome=success|fallback`) — taxa de erro do provedor de
  câmbio, distinta das métricas técnicas de retry/circuit breaker. O caminho
  em que `app.exchange-rate-provider.enabled=false` (provedor desligado,
  nunca chamado) não é contado como `outcome` nenhum — não é uma tentativa
  contra o provedor, contá-lo poluiria a taxa de erro.
- **Resilience4j fica de graça.** `resilience4j-spring-boot3` já registra
  `TaggedCircuitBreakerMetrics`/`TaggedRetryMetrics` automaticamente contra
  qualquer `MeterRegistry` do contexto — só não havia nenhum `MeterRegistry`
  até agora. Adicionar `micrometer-registry-prometheus` ativa essas métricas
  (`resilience4j_circuitbreaker_*`, `resilience4j_retry_*`) retroativamente,
  sem nenhum código novo — é exatamente "conectar a infra existente",
  não duplicá-la.
- **Dashboard do Grafana provisionado via JSON versionado**
  (`observability/grafana/dashboards/srm-credit-engine.json`), não montado
  na UI. `docker compose up` sozinho já sobe com painéis prontos (taxa e
  latência HTTP, liquidações/volume por moeda, taxa de erro do provedor de
  câmbio, estado do circuit breaker, retries) — reproduzível para quem for
  avaliar, sem passo manual.

## Alternativas consideradas

- **Loki/ELK para logs**: mais completo, mas fora de escopo — o enunciado
  pede logs estruturados e correlação, não um segundo backend de
  observabilidade para o avaliador subir.
- **Métricas via `ApplicationEventPublisher`**: desacoplaria produtor de
  consumidor, mas não há fan-out (nenhum outro listener além do próprio
  recorder) — rejeitado como abstração sem uso, mesmo padrão de rejeição já
  aplicado na ADR 0006.
- **Montar o dashboard manualmente após o `docker compose up`**: mais simples
  de implementar, mas deixa o Grafana vazio até alguém configurar os
  painéis na mão — pior para quem for demonstrar ou avaliar o projeto.

## Consequências

- Novo diretório `observability/` (config do Prometheus + provisioning e
  dashboard do Grafana) e dois serviços novos no `docker-compose.yml`
  (`prometheus:9090`, `grafana:3000`, sem volumes de persistência —
  suficiente para uma demonstração, mesmo espírito minimalista do único
  volume `postgres_data` já existente).
- `logging.level.com.srmasset.creditengine` saiu de `application.yml` e
  passou a viver só em `logback-spring.xml`, para não ter duas fontes de
  verdade sobre nível de log.
- Novas métricas de negócio ficam expostas em `/actuator/prometheus` junto
  com as técnicas (HTTP, JVM, Resilience4j) — todas com a tag
  `application=srm-credit-engine`, preparado para múltiplos serviços atrás
  do mesmo Prometheus/Grafana no futuro.
- Adicionar uma métrica nova exige editar o dashboard JSON manualmente —
  aceitável para o escopo atual; nenhum gerador de dashboard-as-code foi
  introduzido.
