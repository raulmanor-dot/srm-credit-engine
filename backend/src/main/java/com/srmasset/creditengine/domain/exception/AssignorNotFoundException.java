package com.srmasset.creditengine.domain.exception;

public class AssignorNotFoundException extends RuntimeException {

    public AssignorNotFoundException(Long id) {
        super("Assignor not found: " + id);
    }
}
