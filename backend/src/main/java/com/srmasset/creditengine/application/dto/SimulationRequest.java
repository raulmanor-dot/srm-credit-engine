package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SimulationRequest(
		@NotNull Long receivableId,
		@NotNull @Positive BigDecimal baseRateMonthlyPercent,
		LocalDate referenceDate) {
}
