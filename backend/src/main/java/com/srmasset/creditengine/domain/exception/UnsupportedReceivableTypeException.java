package com.srmasset.creditengine.domain.exception;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;

public class UnsupportedReceivableTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnsupportedReceivableTypeException(ReceivableTypeCode type) {
        super("No PricingStrategy registered for receivable type: " + type);
    }
}
