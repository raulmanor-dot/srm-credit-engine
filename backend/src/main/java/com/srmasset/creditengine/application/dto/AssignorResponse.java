package com.srmasset.creditengine.application.dto;

import com.srmasset.creditengine.persistence.entity.Assignor;

public record AssignorResponse(Long id, String name, String taxId, boolean active) {

    public static AssignorResponse from(Assignor assignor) {
        return new AssignorResponse(
                assignor.getId(), assignor.getName(), assignor.getTaxId(), assignor.isActive());
    }
}
