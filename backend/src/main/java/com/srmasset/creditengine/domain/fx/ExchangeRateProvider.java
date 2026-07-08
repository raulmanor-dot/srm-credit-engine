package com.srmasset.creditengine.domain.fx;

import com.srmasset.creditengine.domain.exception.ExchangeRateProviderUnavailableException;
import java.math.BigDecimal;

/**
 * Porta para uma fonte externa de cotações em tempo real. O domínio depende só desta interface — a
 * implementação HTTP, o cliente e a resiliência (retry/circuit breaker) vivem no adapter de
 * infraestrutura.
 */
public interface ExchangeRateProvider {

    /**
     * @throws ExchangeRateProviderUnavailableException quando o provedor não responde após as
     *     tentativas configuradas ou o circuito está aberto.
     */
    BigDecimal fetchRate(String baseCurrencyCode, String quoteCurrencyCode);
}
