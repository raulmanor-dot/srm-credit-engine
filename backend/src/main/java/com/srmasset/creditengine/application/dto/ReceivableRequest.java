package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

// due_date > issue_date é reforçado pela constraint chk_receivables_due_after_issue
// no banco (V5), não duplicado aqui como anotação de validação.
public record ReceivableRequest(
		@NotNull Long assignorId,
		@NotNull Long receivableTypeId,
		@NotNull Long faceValueCurrencyId,
		@NotNull @Positive BigDecimal faceValue,
		String documentNumber,
		@NotNull LocalDate issueDate,
		@NotNull LocalDate dueDate) {
}
