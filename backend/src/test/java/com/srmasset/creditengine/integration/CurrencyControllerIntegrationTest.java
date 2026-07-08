package com.srmasset.creditengine.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.CurrencyRequest;
import com.srmasset.creditengine.application.dto.CurrencyUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CurrencyControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createsFindsUpdatesAndDeactivatesACurrency() throws Exception {
		CurrencyRequest createRequest = new CurrencyRequest("EUR", "Euro");

		String body = mockMvc.perform(post("/currencies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value("EUR"))
				.andExpect(jsonPath("$.name").value("Euro"))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long id = objectMapper.readTree(body).get("id").asLong();

		mockMvc.perform(get("/currencies/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.code").value("EUR"));

		CurrencyUpdateRequest updateRequest = new CurrencyUpdateRequest("Euro (renamed)");
		mockMvc.perform(put("/currencies/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Euro (renamed)"));

		mockMvc.perform(delete("/currencies/{id}", id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/currencies/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void returnsConflictForDuplicateCode() throws Exception {
		CurrencyRequest request = new CurrencyRequest("GBP", "Pound Sterling");

		mockMvc.perform(post("/currencies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/currencies")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());
	}

	@Test
	void returnsNotFoundForUnknownCurrency() throws Exception {
		mockMvc.perform(get("/currencies/{id}", 999999L)).andExpect(status().isNotFound());
	}
}
