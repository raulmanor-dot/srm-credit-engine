package com.srmasset.creditengine.mockprovider;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simula um provedor externo de câmbio dentro da própria aplicação: chamado
 * via HTTP real pelo {@code HttpExchangeRateProviderClient}, para que
 * retry/circuit-breaker exercitem timeouts e falhas de rede de verdade, não
 * um stub em memória. Caos (falha/latência) configurável via
 * {@code app.mock-provider.*} — sempre ativo, é o próprio propósito deste
 * endpoint num app de demonstração.
 */
@RestController
@RequestMapping("/mock-provider")
@EnableConfigurationProperties(MockProviderProperties.class)
public class MockExchangeRateProviderController {

	private final MockProviderProperties properties;
	private final MockRateGenerator rateGenerator;

	public MockExchangeRateProviderController(MockProviderProperties properties, MockRateGenerator rateGenerator) {
		this.properties = properties;
		this.rateGenerator = rateGenerator;
	}

	@GetMapping("/rates")
	public ResponseEntity<?> rates(@RequestParam String base, @RequestParam String quote) {
		simulateLatency();

		if (ThreadLocalRandom.current().nextDouble() < properties.failureRate()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(Map.of("error", "mock provider outage (chaos)"));
		}

		return rateGenerator.generate(base, quote)
				.map(rate -> ResponseEntity.ok(new MockRateResponse(base, quote, rate, OffsetDateTime.now())))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private void simulateLatency() {
		long min = properties.minLatencyMs();
		long max = properties.maxLatencyMs();
		if (max <= min) {
			return;
		}
		long delay = ThreadLocalRandom.current().nextLong(min, max + 1);
		if (delay <= 0) {
			return;
		}
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
