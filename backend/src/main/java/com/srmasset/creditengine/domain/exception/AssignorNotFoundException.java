package com.srmasset.creditengine.domain.exception;

public class AssignorNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AssignorNotFoundException(Long id) {
        super("Assignor not found: " + id);
    }
}
