package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CurrencyRequest(@NotBlank @Size(min = 3, max = 3) String code, @NotBlank String name) {
}
