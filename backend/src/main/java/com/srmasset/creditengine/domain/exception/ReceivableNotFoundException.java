package com.srmasset.creditengine.domain.exception;

public class ReceivableNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReceivableNotFoundException(Long id) {
        super("Receivable not found: " + id);
    }
}
