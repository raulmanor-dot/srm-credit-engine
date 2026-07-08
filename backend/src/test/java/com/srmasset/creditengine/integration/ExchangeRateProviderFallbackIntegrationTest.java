package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.SettlementRequest;
import com.srmasset.creditengine.domain.fx.ExchangeRateProvider;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.infrastructure.fx.HttpExchangeRateProviderClient;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.entity.Settlement;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ExchangeRateRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import com.srmasset.creditengine.persistence.repository.SettlementRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Mock provider sempre falha ({@code failure-rate=1.0}): a liquidação nunca
 * pode travar por isso — deve cair para a última taxa conhecida no banco.
 * Roda numa porta fixa própria (18082) para não colidir com o contexto do
 * teste "happy path" (cada {@code @SpringBootTest} distinto sobe seu próprio
 * ApplicationContext e servidor embutido).
 *
 * <p>Usa um par de moedas dedicado (GBP/CHF, com uma taxa MANUAL seedada
 * pelo próprio teste) em vez de USD/BRL — mesma razão do teste "happy path":
 * evitar contaminar o par que outros testes assumem ser exatamente o
 * seedado em V7, já que {@code exchange_rates} é append-only e o Postgres
 * é compartilhado por toda a suíte.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = {
		"server.port=18082",
		"app.exchange-rate-provider.enabled=true",
		"app.exchange-rate-provider.base-url=http://localhost:18082",
		"app.mock-provider.failure-rate=1.0",
		"app.mock-provider.max-latency-ms=0"})
class ExchangeRateProviderFallbackIntegrationTest extends AbstractIntegrationTest {

	private static final BigDecimal SEEDED_GBP_CHF_RATE = new BigDecimal("1.250000");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CurrencyRepository currencyRepository;

	@Autowired
	private ReceivableTypeRepository receivableTypeRepository;

	@Autowired
	private AssignorRepository assignorRepository;

	@Autowired
	private ReceivableRepository receivableRepository;

	@Autowired
	private SettlementRepository settlementRepository;

	@Autowired
	private ExchangeRateRepository exchangeRateRepository;

	@Autowired
	private ExchangeRateProvider exchangeRateProvider;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	private Currency gbp;
	private Currency chf;

	@BeforeEach
	void setUp() {
		circuitBreakerRegistry.circuitBreaker(HttpExchangeRateProviderClient.RESILIENCE_INSTANCE).reset();
		gbp = currencyRepository.findByCode("GBP").orElseGet(() -> currencyRepository.save(new Currency("GBP", "Libra Esterlina")));
		chf = currencyRepository.findByCode("CHF").orElseGet(() -> currencyRepository.save(new Currency("CHF", "Franco Suico")));
		exchangeRateRepository.save(new ExchangeRate(gbp, chf, SEEDED_GBP_CHF_RATE, ExchangeRate.Source.MANUAL));
	}

	private Receivable newPendingReceivable(String documentNumber, LocalDate referenceDate) {
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL).orElseThrow();
		Assignor assignor = assignorRepository.save(new Assignor("Empresa " + documentNumber, "1" + documentNumber.hashCode() + "000199"));
		return receivableRepository.save(new Receivable(
				assignor, duplicata, gbp, new BigDecimal("10000.00"), documentNumber, referenceDate, referenceDate.plusDays(30)));
	}

	@Test
	void settlementFallsBackToStoredSeedRateWhenProviderAlwaysFails() throws Exception {
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = newPendingReceivable("DOC-FALLBACK-1", referenceDate);
		long ratesBefore = exchangeRateRepository.count();

		SettlementRequest request = new SettlementRequest(receivable.getId(), "CHF", new BigDecimal("2.0"), referenceDate);
		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		Settlement settlement = settlementRepository.findByReceivableId(receivable.getId()).orElseThrow();
		assertThat(settlement.getFxRateUsed()).isEqualByComparingTo(SEEDED_GBP_CHF_RATE);
		assertThat(exchangeRateRepository.count()).isEqualTo(ratesBefore);

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(HttpExchangeRateProviderClient.RESILIENCE_INSTANCE);
		assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
	}

	@Test
	void circuitBreakerOpensAfterRepeatedFailuresAndSettlementStillSucceeds() throws Exception {
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(HttpExchangeRateProviderClient.RESILIENCE_INSTANCE);

		for (int i = 0; i < 6 && circuitBreaker.getState() != CircuitBreaker.State.OPEN; i++) {
			try {
				exchangeRateProvider.fetchRate("GBP", "CHF");
			} catch (Exception ignored) {
				// esperado: provedor sempre falha nesta suíte
			}
		}
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = newPendingReceivable("DOC-FALLBACK-2", referenceDate);
		SettlementRequest request = new SettlementRequest(receivable.getId(), "CHF", new BigDecimal("2.0"), referenceDate);

		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		Settlement settlement = settlementRepository.findByReceivableId(receivable.getId()).orElseThrow();
		assertThat(settlement.getFxRateUsed()).isEqualByComparingTo(SEEDED_GBP_CHF_RATE);
	}
}
