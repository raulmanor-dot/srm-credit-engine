package com.srmasset.creditengine.domain.exception;

public class ReceivableNotFoundException extends RuntimeException {

    public ReceivableNotFoundException(Long id) {
        super("Receivable not found: " + id);
    }
}
