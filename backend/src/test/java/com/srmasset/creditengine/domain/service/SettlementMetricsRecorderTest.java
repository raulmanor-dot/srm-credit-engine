package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SettlementMetricsRecorderTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SettlementMetricsRecorder recorder = new SettlementMetricsRecorder(registry);

    @Test
    void recordsCountAndVolumeTaggedByCurrency() {
        recorder.recordSettlement("USD", new BigDecimal("1000.50"));
        recorder.recordSettlement("USD", new BigDecimal("500.00"));
        recorder.recordSettlement("BRL", new BigDecimal("300.00"));

        assertThat(registry.get("settlements.count").tag("currency", "USD").counter().count())
                .isEqualTo(2.0);
        assertThat(registry.get("settlements.count").tag("currency", "BRL").counter().count())
                .isEqualTo(1.0);
        assertThat(
                        registry.get("settlements.volume")
                                .tag("currency", "USD")
                                .summary()
                                .totalAmount())
                .isEqualTo(1500.50);
    }
}
