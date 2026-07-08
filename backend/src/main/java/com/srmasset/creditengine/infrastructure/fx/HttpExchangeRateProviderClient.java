package com.srmasset.creditengine.infrastructure.fx;

import com.srmasset.creditengine.domain.exception.ExchangeRateProviderUnavailableException;
import com.srmasset.creditengine.domain.fx.ExchangeRateProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adapter HTTP para o provedor mock (ver {@code mockprovider} package): chamada real via {@link
 * RestClient}, protegida por Resilience4j. Retry (mais externo) tenta de novo em falhas
 * transitórias; CircuitBreaker registra cada tentativa e abre o circuito após falhas repetidas,
 * poupando o provedor (e o chamador) de continuar tentando enquanto ele está fora do ar. Quando as
 * tentativas se esgotam ou o circuito está aberto, o fallback traduz a falha em {@link
 * ExchangeRateProviderUnavailableException} — nunca propaga a exceção técnica (HTTP/timeout) para o
 * domínio.
 */
@Component
public class HttpExchangeRateProviderClient implements ExchangeRateProvider {

    public static final String RESILIENCE_INSTANCE = "exchange-rate-provider";

    private final RestClient restClient;

    public HttpExchangeRateProviderClient(RestClient exchangeRateProviderRestClient) {
        this.restClient = exchangeRateProviderRestClient;
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE, fallbackMethod = "providerUnavailable")
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public BigDecimal fetchRate(String baseCurrencyCode, String quoteCurrencyCode) {
        ProviderRateResponse response =
                restClient
                        .get()
                        .uri(
                                "/mock-provider/rates?base={base}&quote={quote}",
                                baseCurrencyCode,
                                quoteCurrencyCode)
                        .retrieve()
                        .body(ProviderRateResponse.class);
        if (response == null || response.rate() == null) {
            throw new IllegalStateException(
                    "Empty response from FX provider for "
                            + baseCurrencyCode
                            + "->"
                            + quoteCurrencyCode);
        }
        return response.rate();
    }

    @SuppressWarnings("unused")
    private BigDecimal providerUnavailable(
            String baseCurrencyCode, String quoteCurrencyCode, Throwable cause) {
        throw new ExchangeRateProviderUnavailableException(
                baseCurrencyCode, quoteCurrencyCode, cause);
    }
}
