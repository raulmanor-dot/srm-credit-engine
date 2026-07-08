package com.srmasset.creditengine.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Taxa de erro do provedor de câmbio (métrica de negócio: sucesso vs. fallback), distinta das
 * métricas técnicas de retry/circuit breaker que o Resilience4j já expõe automaticamente via
 * Micrometer — ver ADR 0007.
 */
@Component
public class ExchangeRateMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public ExchangeRateMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordOutcome(String outcome) {
        Counter.builder("fx.provider.requests")
                .tag("outcome", outcome)
                .description(
                        "Resultado de cada tentativa de obter cotação do provedor de câmbio (success|fallback)")
                .register(meterRegistry)
                .increment();
    }
}
