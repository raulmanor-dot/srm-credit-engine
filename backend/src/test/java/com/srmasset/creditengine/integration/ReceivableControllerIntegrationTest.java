package com.srmasset.creditengine.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.ReceivableRequest;
import com.srmasset.creditengine.application.dto.ReceivableUpdateRequest;
import com.srmasset.creditengine.application.dto.SettlementRequest;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ReceivableControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private CurrencyRepository currencyRepository;

    @Autowired private ReceivableTypeRepository receivableTypeRepository;

    @Autowired private AssignorRepository assignorRepository;

    @Test
    void createsFindsUpdatesAndCancelsAReceivable() throws Exception {
        Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
        ReceivableType duplicata =
                receivableTypeRepository
                        .findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL)
                        .orElseThrow();
        Assignor assignor = assignorRepository.save(new Assignor("Empresa CRUD", "22333444000155"));

        LocalDate issueDate = LocalDate.of(2026, 7, 1);
        LocalDate dueDate = issueDate.plusDays(30);
        ReceivableRequest createRequest =
                new ReceivableRequest(
                        assignor.getId(),
                        duplicata.getId(),
                        brl.getId(),
                        new BigDecimal("5000.00"),
                        "DOC-CRUD-1",
                        issueDate,
                        dueDate);

        String body =
                mockMvc.perform(
                                post("/receivables")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.assignorId").value(assignor.getId()))
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.faceValueCurrencyCode").value("BRL"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/receivables/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("DOC-CRUD-1"));

        ReceivableUpdateRequest updateRequest =
                new ReceivableUpdateRequest(
                        new BigDecimal("5500.00"), "DOC-CRUD-1-REV", issueDate, dueDate);
        mockMvc.perform(
                        put("/receivables/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faceValue").value(5500.00))
                .andExpect(jsonPath("$.documentNumber").value("DOC-CRUD-1-REV"));

        mockMvc.perform(delete("/receivables/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/receivables/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void rejectsUpdatingAndCancelingAlreadySettledReceivable() throws Exception {
        Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
        ReceivableType duplicata =
                receivableTypeRepository
                        .findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL)
                        .orElseThrow();
        Assignor assignor =
                assignorRepository.save(new Assignor("Empresa CRUD Settled", "33444555000166"));

        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        ReceivableRequest createRequest =
                new ReceivableRequest(
                        assignor.getId(),
                        duplicata.getId(),
                        brl.getId(),
                        new BigDecimal("1000.00"),
                        "DOC-CRUD-2",
                        referenceDate,
                        referenceDate.plusDays(30));

        String body =
                mockMvc.perform(
                                post("/receivables")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(createRequest)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        SettlementRequest settlementRequest =
                new SettlementRequest(id, "BRL", new BigDecimal("2.0"), referenceDate);
        mockMvc.perform(
                        post("/settlements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(settlementRequest)))
                .andExpect(status().isOk());

        ReceivableUpdateRequest updateRequest =
                new ReceivableUpdateRequest(
                        new BigDecimal("2000.00"),
                        "DOC-CRUD-2",
                        referenceDate,
                        referenceDate.plusDays(30));
        mockMvc.perform(
                        put("/receivables/{id}", id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/receivables/{id}", id)).andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForUnknownReceivable() throws Exception {
        mockMvc.perform(get("/receivables/{id}", 999999L)).andExpect(status().isNotFound());
    }
}
