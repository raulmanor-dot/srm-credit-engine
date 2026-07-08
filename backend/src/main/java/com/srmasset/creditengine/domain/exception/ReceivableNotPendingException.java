package com.srmasset.creditengine.domain.exception;

import com.srmasset.creditengine.persistence.entity.Receivable;

public class ReceivableNotPendingException extends RuntimeException {

    public ReceivableNotPendingException(Long id, Receivable.Status currentStatus) {
        super("Receivable " + id + " is not PENDING (current status: " + currentStatus + ")");
    }
}
