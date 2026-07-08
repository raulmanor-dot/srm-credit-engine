package com.srmasset.creditengine.domain.exception;

public class ReceivableTypeNotFoundException extends RuntimeException {

    public ReceivableTypeNotFoundException(Long id) {
        super("Receivable type not found: " + id);
    }
}
