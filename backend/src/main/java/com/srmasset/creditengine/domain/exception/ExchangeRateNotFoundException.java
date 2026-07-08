package com.srmasset.creditengine.domain.exception;

import com.srmasset.creditengine.persistence.entity.Currency;

public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(Currency base, Currency quote) {
        super(
                "No exchange rate found between "
                        + base.getCode()
                        + " and "
                        + quote.getCode()
                        + " (checked both directions)");
    }
}
