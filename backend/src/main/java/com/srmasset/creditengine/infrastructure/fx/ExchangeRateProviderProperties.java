package com.srmasset.creditengine.infrastructure.fx;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.exchange-rate-provider")
public record ExchangeRateProviderProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("http://localhost:8080") String baseUrl,
		@DefaultValue("500ms") Duration connectTimeout,
		@DefaultValue("2s") Duration readTimeout) {
}
