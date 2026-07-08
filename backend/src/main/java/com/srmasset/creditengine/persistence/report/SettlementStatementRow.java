package com.srmasset.creditengine.persistence.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Projeção plana do extrato de liquidação — não é uma entidade JPA. Espelha
 * exatamente as colunas retornadas pela query SQL nativa de
 * {@link SettlementReportRepository}.
 */
public record SettlementStatementRow(
		Long settlementId,
		String documentNumber,
		String assignorName,
		BigDecimal faceValue,
		String faceValueCurrencyCode,
		BigDecimal presentValueFaceCurrency,
		BigDecimal netValuePaymentCurrency,
		String paymentCurrencyCode,
		BigDecimal baseRateUsed,
		BigDecimal spreadUsed,
		BigDecimal fxRateUsed,
		OffsetDateTime settledAt) {
}
