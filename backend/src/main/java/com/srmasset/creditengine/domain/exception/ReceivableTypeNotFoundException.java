package com.srmasset.creditengine.domain.exception;

public class ReceivableTypeNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReceivableTypeNotFoundException(Long id) {
        super("Receivable type not found: " + id);
    }
}
