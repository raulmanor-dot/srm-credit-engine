package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignorRequest(@NotBlank String name, @NotBlank String taxId) {}
