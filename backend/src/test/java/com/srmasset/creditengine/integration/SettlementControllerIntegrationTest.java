package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.config.CorrelationIdFilter;
import com.srmasset.creditengine.application.dto.SettlementRequest;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.domain.service.SettlementService;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.entity.Settlement;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import com.srmasset.creditengine.persistence.repository.SettlementRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SettlementControllerIntegrationTest extends AbstractIntegrationTest {

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
	private SettlementService settlementService;

	@Autowired
	private MeterRegistry meterRegistry;

	private Currency brl;
	private ReceivableType duplicata;

	private void loadReferenceData() {
		brl = currencyRepository.findByCode("BRL").orElseThrow();
		duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL).orElseThrow();
	}

	private Receivable newPendingReceivable(Currency faceCurrency, String documentNumber, LocalDate referenceDate) {
		Assignor assignor = assignorRepository.save(new Assignor("Empresa " + documentNumber, "1" + documentNumber.hashCode() + "000199"));
		return receivableRepository.save(new Receivable(
				assignor, duplicata, faceCurrency, new BigDecimal("10000.00"), documentNumber, referenceDate, referenceDate.plusDays(30)));
	}

	@Test
	void settlesInSameCurrencyWithNoFxConversion() throws Exception {
		loadReferenceData();
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = newPendingReceivable(brl, "DOC-SAME-1", referenceDate);

		SettlementRequest request = new SettlementRequest(receivable.getId(), "BRL", new BigDecimal("2.0"), referenceDate);

		double countBefore = meterRegistry.get("settlements.count").tag("currency", "BRL").counter().count();

		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(header().exists(CorrelationIdFilter.REQUEST_ID_HEADER))
				.andExpect(jsonPath("$.receivableId").value(receivable.getId()))
				.andExpect(jsonPath("$.paymentCurrencyCode").value("BRL"));

		Settlement settlement = settlementRepository.findByReceivableId(receivable.getId()).orElseThrow();
		assertThat(settlement.getFxRateUsed()).isNull();
		assertThat(settlement.getNetValuePaymentCurrency()).isEqualByComparingTo(settlement.getPresentValueFaceCurrency());

		Receivable reloaded = receivableRepository.findById(receivable.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(Receivable.Status.SETTLED);

		double countAfter = meterRegistry.get("settlements.count").tag("currency", "BRL").counter().count();
		assertThat(countAfter).isEqualTo(countBefore + 1);
	}

	@Test
	void settlesWithInverseFxLookupWhenOnlyOppositeDirectionIsSeeded() throws Exception {
		loadReferenceData();
		// Seed V7 só tem USD -> BRL (5.4). Liquidar um recebível em BRL pago em
		// USD exercita exatamente o lookup inverso do ExchangeRateService.
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = newPendingReceivable(brl, "DOC-INV-1", referenceDate);

		SettlementRequest request = new SettlementRequest(receivable.getId(), "USD", new BigDecimal("2.0"), referenceDate);

		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paymentCurrencyCode").value("USD"));

		Settlement settlement = settlementRepository.findByReceivableId(receivable.getId()).orElseThrow();
		BigDecimal expectedInverseRate = BigDecimal.ONE.divide(new BigDecimal("5.4"), 6, RoundingMode.HALF_EVEN);
		assertThat(settlement.getFxRateUsed()).isEqualByComparingTo(expectedInverseRate);
	}

	@Test
	void rejectsSettlingAnAlreadySettledReceivable() throws Exception {
		loadReferenceData();
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable = newPendingReceivable(brl, "DOC-DUP-1", referenceDate);
		SettlementRequest request = new SettlementRequest(receivable.getId(), "BRL", new BigDecimal("2.0"), referenceDate);

		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk());

		mockMvc.perform(post("/settlements")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());
	}

	@Test
	void batchProcessesEachItemIndependently() {
		loadReferenceData();
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		Receivable receivable1 = newPendingReceivable(brl, "DOC-BATCH-1", referenceDate);
		Receivable receivable2 = newPendingReceivable(brl, "DOC-BATCH-2", referenceDate);
		Receivable receivable3 = newPendingReceivable(brl, "DOC-BATCH-3", referenceDate);

		// Pré-liquida o item 2 individualmente para forçar uma falha
		// determinística no meio do lote, sem depender de concorrência real.
		settlementService.settle(receivable2.getId(), "BRL", new BaseRate(new BigDecimal("2.0"), referenceDate));

		var results = new com.srmasset.creditengine.domain.service.SettlementBatchService(settlementService)
				.settleBatch(
						List.of(receivable1.getId(), receivable2.getId(), receivable3.getId()),
						"BRL",
						new BaseRate(new BigDecimal("2.0"), referenceDate));

		assertThat(results).hasSize(3);
		assertThat(results.get(0).success()).isTrue();
		assertThat(results.get(1).success()).isFalse();
		assertThat(results.get(2).success()).isTrue();

		assertThat(settlementRepository.findByReceivableId(receivable1.getId())).isPresent();
		assertThat(settlementRepository.findByReceivableId(receivable3.getId())).isPresent();
	}
}
