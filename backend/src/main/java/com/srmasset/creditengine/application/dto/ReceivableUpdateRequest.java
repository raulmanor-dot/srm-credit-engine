package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableUpdateRequest(
        @NotNull @Positive BigDecimal faceValue,
        String documentNumber,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate) {}
