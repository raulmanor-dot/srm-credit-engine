package com.srmasset.creditengine.integration;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.SimulationRequest;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ponta a ponta contra Postgres real (Testcontainers): grava um recebível via
 * repositório (ainda não há controller de CRUD), chama POST /simulations e
 * confere que o valor presente calculado bate com a conta manual — provando
 * que o motor de precificação, o schema e o endpoint estão corretamente
 * ligados.
 */
@AutoConfigureMockMvc
class SimulationControllerIntegrationTest extends AbstractIntegrationTest {

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

	@Test
	void simulatesPresentValueForAPersistedReceivable() throws Exception {
		Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL)
				.orElseThrow();
		Assignor assignor = assignorRepository.save(new Assignor("Empresa Teste", "11222333000144"));

		LocalDate referenceDate = LocalDate.of(2026, 7, 7);
		LocalDate dueDate = referenceDate.plusDays(30);
		Receivable receivable = receivableRepository.save(new Receivable(
				assignor, duplicata, brl, new BigDecimal("10000.00"), "DOC-IT-1", referenceDate, dueDate));

		SimulationRequest request = new SimulationRequest(receivable.getId(), new BigDecimal("2.0"), referenceDate);

		// taxa total = 2,0% (base) + 1,5% (spread da duplicata) = 3,5% a.m.; prazo = 1 mes
		// PV = 10000 / 1,035 = 9661,8357487922... -> 9661,835749 (HALF_EVEN, 6 casas)
		mockMvc.perform(post("/simulations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.receivableId").value(receivable.getId()))
				.andExpect(jsonPath("$.presentValue").value(comparesEqualTo(9661.835749)))
				.andExpect(jsonPath("$.termInMonths").value(comparesEqualTo(1.0)))
				.andExpect(jsonPath("$.currencyCode").value("BRL"));
	}

	@Test
	void returnsNotFoundForUnknownReceivable() throws Exception {
		SimulationRequest request = new SimulationRequest(999999L, new BigDecimal("2.0"), LocalDate.now());

		mockMvc.perform(post("/simulations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isNotFound());
	}
}
