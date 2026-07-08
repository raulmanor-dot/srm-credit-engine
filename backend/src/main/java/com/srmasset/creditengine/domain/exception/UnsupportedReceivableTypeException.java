package com.srmasset.creditengine.domain.exception;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;

public class UnsupportedReceivableTypeException extends RuntimeException {

    public UnsupportedReceivableTypeException(ReceivableTypeCode type) {
        super("No PricingStrategy registered for receivable type: " + type);
    }
}
