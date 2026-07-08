package com.srmasset.creditengine.domain.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio de liquidação, registradas em um único ponto ({@link
 * SettlementService#settle}) para não duplicar a instrumentação entre o caminho do controller e o
 * caminho de lote ({@code SettlementBatchService}) — ver ADR 0007.
 */
@Component
public class SettlementMetricsRecorder {

    private final MeterRegistry meterRegistry;

    public SettlementMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSettlement(String paymentCurrencyCode, BigDecimal netValuePaymentCurrency) {
        Counter.builder("settlements.count")
                .tag("currency", paymentCurrencyCode)
                .description("Número de liquidações concluídas, por moeda de pagamento")
                .register(meterRegistry)
                .increment();

        meterRegistry
                .summary("settlements.volume", "currency", paymentCurrencyCode)
                .record(netValuePaymentCurrency.doubleValue());
    }
}
