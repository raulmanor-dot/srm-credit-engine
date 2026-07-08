package com.srmasset.creditengine.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regressão: o catch-all de {@code Exception} no {@code GlobalExceptionHandler} chegou a
 * transformar {@code NoResourceFoundException} (rota inexistente) num 500, incluindo em GET / — a
 * primeira rota que qualquer pessoa abrindo a API pela primeira vez tende a tentar.
 */
@AutoConfigureMockMvc
class RootAndNotFoundIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void rootPathRedirectsToSwaggerUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }

    @Test
    void unknownPathReturnsNotFoundNotServerError() throws Exception {
        mockMvc.perform(get("/this-route-does-not-exist")).andExpect(status().isNotFound());
    }
}
