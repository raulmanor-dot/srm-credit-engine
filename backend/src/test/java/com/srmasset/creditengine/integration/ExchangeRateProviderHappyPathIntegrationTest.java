package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.SettlementRequest;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Sobe um servidor HTTP real numa porta fixa (necessário para que
 * {@code HttpExchangeRateProviderClient} consiga de fato chamar
 * {@code /mock-provider/rates} da própria aplicação — {@code MockMvc}
 * sozinho, sem porta ouvindo, não seria suficiente). Sem caos (failure-rate
 * 0.0) para exercitar o caminho feliz de forma determinística.
 *
 * <p>Usa um par de moedas dedicado (EUR/JPY, criado aqui, não seedado pela
 * V7) em vez de USD/BRL: como {@code exchange_rates} é append-only e o
 * Postgres do Testcontainers é um singleton compartilhado por toda a suíte
 * (ver {@link AbstractIntegrationTest}), inserir uma cotação fresca para
 * USD/BRL contaminaria permanentemente o "latest rate" que outros testes
 * (ex.: {@code SettlementControllerIntegrationTest}) esperam ser exatamente
 * o valor seedado em V7.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = {
		"server.port=18081",
		"app.exchange-rate-provider.enabled=true",
		"app.exchange-rate-provider.base-url=http://localhost:18081",
		"app.mock-provider.failure-rate=0.0",
		"app.mock-provider.max-latency-ms=0",
		"app.mock-provider.rates.EUR-JPY=165.500000"})
class ExchangeRateProviderHappyPathIntegrationTest extends AbstractIntegrationTest {

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

	@Test
	void mockProviderEndpointReturnsJitteredRate() throws Exception {
		mockMvc.perform(get("/mock-provider/rates").param("base", "EUR").param("quote", "JPY"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.base").value("EUR"))
				.andExpect(jsonPath("$.quote").value("JPY"));
	}

	@Test
	void mockProviderReturns404ForUnknownPair() throws Exception {
		mockMvc.perform(get("/mock-provider/rates").param("base", "XAU").param("quote", "XAG"))
				.andExpect(status().isNotFound());
	}

	@Test
	void crossCurrencySettlementPersistsFreshProviderRateAppendOnly() throws Exception {
		Currency eur = currencyRepository.findByCode("EUR").orElseGet(() -> currencyRepository.save(new Currency("EUR", "Euro")));
		currencyRepository.findByCode("JPY").orElseGet(() -> currencyRepository.save(new Currency("JPY", "Iene Japones")));
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL).orElseThrow();
		Assignor assignor = assignorRepository.save(new Assignor("Empresa Happy Path", "11222333000199"));
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = receivableRepository.save(new Receivable(
				assignor, duplicata, eur, new BigDecimal("10000.00"), "DOC-PROVIDER-1", referenceDate, referenceDate.plusDays(30)));

		long ratesBefore = exchangeRateRepository.count();

		SettlementRequest request = new SettlementRequest(receivable.getId(), "JPY", new BigDecimal("2.0"), referenceDate);
		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		Settlement settlement = settlementRepository.findByReceivableId(receivable.getId()).orElseThrow();
		assertThat(settlement.getFxRateUsed()).isNotNull();

		long ratesAfter = exchangeRateRepository.count();
		assertThat(ratesAfter).isEqualTo(ratesBefore + 1);
		ExchangeRate newestRate = exchangeRateRepository.findAll().stream()
				.max(Comparator.comparing(ExchangeRate::getValidFrom))
				.orElseThrow();
		assertThat(newestRate.getSource()).isEqualTo(ExchangeRate.Source.MOCK_PROVIDER);
		assertThat(settlement.getFxRateUsed()).isEqualByComparingTo(newestRate.getRate());
	}
}
