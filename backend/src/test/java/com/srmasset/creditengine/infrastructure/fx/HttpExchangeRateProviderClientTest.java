package com.srmasset.creditengine.infrastructure.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Testa só o parsing/tradução de resposta do adapter, sem contexto Spring — as anotações
 * {@code @Retry}/{@code @CircuitBreaker} são inertes sem o proxy AOP (isso é coberto pelos testes
 * de integração com servidor real).
 */
class HttpExchangeRateProviderClientTest {

    @Test
    void returnsRateFromSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/mock-provider/rates?base=USD&quote=BRL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                "{\"base\":\"USD\",\"quote\":\"BRL\",\"rate\":5.45,\"timestamp\":\"2026-07-08T10:00:00Z\"}",
                                MediaType.APPLICATION_JSON));
        HttpExchangeRateProviderClient client = new HttpExchangeRateProviderClient(builder.build());

        BigDecimal result = client.fetchRate("USD", "BRL");

        assertThat(result).isEqualByComparingTo(new BigDecimal("5.45"));
    }

    @Test
    void propagatesRestClientExceptionOn503() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("/mock-provider/rates?base=USD&quote=BRL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());
        HttpExchangeRateProviderClient client = new HttpExchangeRateProviderClient(builder.build());

        assertThatThrownBy(() -> client.fetchRate("USD", "BRL"))
                .isInstanceOf(RestClientException.class);
    }
}
