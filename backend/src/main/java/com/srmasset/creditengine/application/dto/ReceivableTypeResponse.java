package com.srmasset.creditengine.application.dto;

import com.srmasset.creditengine.persistence.entity.ReceivableType;
import java.math.BigDecimal;

public record ReceivableTypeResponse(Long id, String code, String name, BigDecimal spreadPercentMonthly) {

	public static ReceivableTypeResponse from(ReceivableType receivableType) {
		return new ReceivableTypeResponse(
				receivableType.getId(),
				receivableType.getCode().name(),
				receivableType.getName(),
				receivableType.getSpreadPercentMonthly());
	}
}
