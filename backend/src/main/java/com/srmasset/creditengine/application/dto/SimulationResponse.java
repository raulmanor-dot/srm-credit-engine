package com.srmasset.creditengine.application.dto;

import java.math.BigDecimal;

public record SimulationResponse(
		Long receivableId,
		BigDecimal faceValue,
		BigDecimal presentValue,
		BigDecimal termInMonths,
		String currencyCode) {
}
