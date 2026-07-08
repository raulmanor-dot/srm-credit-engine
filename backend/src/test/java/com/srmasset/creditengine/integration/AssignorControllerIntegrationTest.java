package com.srmasset.creditengine.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srmasset.creditengine.application.dto.AssignorRequest;
import com.srmasset.creditengine.application.dto.AssignorUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AssignorControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createsFindsUpdatesAndDeactivatesAnAssignor() throws Exception {
		AssignorRequest createRequest = new AssignorRequest("Empresa Alpha", "11222333000144");

		String body = mockMvc.perform(post("/assignors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createRequest)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Empresa Alpha"))
				.andExpect(jsonPath("$.taxId").value("11222333000144"))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn()
				.getResponse()
				.getContentAsString();

		Long id = objectMapper.readTree(body).get("id").asLong();

		mockMvc.perform(get("/assignors/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Empresa Alpha"));

		AssignorUpdateRequest updateRequest = new AssignorUpdateRequest("Empresa Alpha Ltda");
		mockMvc.perform(put("/assignors/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updateRequest)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Empresa Alpha Ltda"));

		mockMvc.perform(delete("/assignors/{id}", id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/assignors/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void returnsConflictForDuplicateTaxId() throws Exception {
		AssignorRequest request = new AssignorRequest("Empresa Beta", "99888777000166");

		mockMvc.perform(post("/assignors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/assignors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict());
	}

	@Test
	void returnsNotFoundForUnknownAssignor() throws Exception {
		mockMvc.perform(get("/assignors/{id}", 999999L)).andExpect(status().isNotFound());
	}
}
