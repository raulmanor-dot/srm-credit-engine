package com.srmasset.creditengine.domain.exception;

/**
 * Traduz o esgotamento de retries ou um circuito aberto (Resilience4j) em um sinal de domínio.
 * Nunca escapa até a API: {@code ExchangeRateService} sempre a captura e recorre à última taxa
 * conhecida no banco.
 */
public class ExchangeRateProviderUnavailableException extends RuntimeException {

    public ExchangeRateProviderUnavailableException(
            String baseCurrencyCode, String quoteCurrencyCode, Throwable cause) {
        super(
                "Exchange rate provider unavailable for "
                        + baseCurrencyCode
                        + "->"
                        + quoteCurrencyCode,
                cause);
    }
}
