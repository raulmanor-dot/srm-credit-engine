package com.srmasset.creditengine.domain.exception;

import com.srmasset.creditengine.persistence.entity.Receivable;

public class ReceivableNotPendingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReceivableNotPendingException(Long id, Receivable.Status currentStatus) {
        super("Receivable " + id + " is not PENDING (current status: " + currentStatus + ")");
    }
}
