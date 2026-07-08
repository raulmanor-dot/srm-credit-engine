package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ExchangeRateMetricsRecorderTest {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final ExchangeRateMetricsRecorder recorder = new ExchangeRateMetricsRecorder(registry);

	@Test
	void recordsOutcomesTaggedSeparately() {
		recorder.recordOutcome("success");
		recorder.recordOutcome("success");
		recorder.recordOutcome("fallback");

		assertThat(registry.get("fx.provider.requests").tag("outcome", "success").counter().count()).isEqualTo(2.0);
		assertThat(registry.get("fx.provider.requests").tag("outcome", "fallback").counter().count()).isEqualTo(1.0);
	}
}
