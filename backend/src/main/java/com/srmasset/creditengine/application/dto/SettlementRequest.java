package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementRequest(
		@NotNull Long receivableId,
		@NotBlank String paymentCurrencyCode,
		@NotNull @Positive BigDecimal baseRateMonthlyPercent,
		LocalDate referenceDate) {
}
