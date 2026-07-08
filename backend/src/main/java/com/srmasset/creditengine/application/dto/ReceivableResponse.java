package com.srmasset.creditengine.application.dto;

import com.srmasset.creditengine.persistence.entity.Receivable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ReceivableResponse(
		Long id,
		Long assignorId,
		String assignorName,
		Long receivableTypeId,
		String receivableTypeCode,
		String faceValueCurrencyCode,
		BigDecimal faceValue,
		String documentNumber,
		LocalDate issueDate,
		LocalDate dueDate,
		Receivable.Status status,
		Long version,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {

	public static ReceivableResponse from(Receivable receivable) {
		return new ReceivableResponse(
				receivable.getId(),
				receivable.getAssignor().getId(),
				receivable.getAssignor().getName(),
				receivable.getReceivableType().getId(),
				receivable.getReceivableType().getCode().name(),
				receivable.getFaceValueCurrency().getCode(),
				receivable.getFaceValue(),
				receivable.getDocumentNumber(),
				receivable.getIssueDate(),
				receivable.getDueDate(),
				receivable.getStatus(),
				receivable.getVersion(),
				receivable.getCreatedAt(),
				receivable.getUpdatedAt());
	}
}
