package com.srmasset.creditengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "receivables")
public class Receivable {

	public enum Status {
		PENDING,
		SETTLED,
		CANCELED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "assignor_id")
	private Assignor assignor;

	@ManyToOne(optional = false)
	@JoinColumn(name = "receivable_type_id")
	private ReceivableType receivableType;

	@ManyToOne(optional = false)
	@JoinColumn(name = "face_value_currency_id")
	private Currency faceValueCurrency;

	@Column(name = "face_value", nullable = false, precision = 19, scale = 6)
	private BigDecimal faceValue;

	@Column(name = "document_number")
	private String documentNumber;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.PENDING;

	// Optimistic locking: concorrência na liquidação em lote é rejeitada aqui
	// (defesa em profundidade junto com a constraint UNIQUE em settlements.receivable_id).
	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected Receivable() {
	}

	public Receivable(
			Assignor assignor,
			ReceivableType receivableType,
			Currency faceValueCurrency,
			BigDecimal faceValue,
			String documentNumber,
			LocalDate issueDate,
			LocalDate dueDate) {
		this.assignor = assignor;
		this.receivableType = receivableType;
		this.faceValueCurrency = faceValueCurrency;
		this.faceValue = faceValue;
		this.documentNumber = documentNumber;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
	}

	public Long getId() {
		return id;
	}

	public Assignor getAssignor() {
		return assignor;
	}

	public ReceivableType getReceivableType() {
		return receivableType;
	}

	public Currency getFaceValueCurrency() {
		return faceValueCurrency;
	}

	public BigDecimal getFaceValue() {
		return faceValue;
	}

	public String getDocumentNumber() {
		return documentNumber;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public Status getStatus() {
		return status;
	}

	public Long getVersion() {
		return version;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}
}
