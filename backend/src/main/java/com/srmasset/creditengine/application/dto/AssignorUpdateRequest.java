package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignorUpdateRequest(@NotBlank String name) {}
