package com.srmasset.creditengine.mockprovider;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Knobs de caos do provedor mock: cada liquidação cross-currency que passa
 * por aqui pode falhar (simulando outage), atrasar (simulando rede lenta) ou
 * variar levemente a cotação (simulando um provedor real de câmbio).
 */
@ConfigurationProperties(prefix = "app.mock-provider")
public record MockProviderProperties(
		@DefaultValue("0.2") double failureRate,
		@DefaultValue("0") long minLatencyMs,
		@DefaultValue("0") long maxLatencyMs,
		@DefaultValue("2.0") double jitterPercent,
		Map<String, BigDecimal> rates) {
}
