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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Linha de uma série temporal append-only: nunca é atualizada (o próprio
 * banco rejeita UPDATE/DELETE via trigger). Uma nova cotação = uma nova linha.
 */
@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {

	public enum Source {
		MANUAL,
		MOCK_PROVIDER
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "base_currency_id")
	private Currency baseCurrency;

	@ManyToOne(optional = false)
	@JoinColumn(name = "quote_currency_id")
	private Currency quoteCurrency;

	@Column(nullable = false, precision = 19, scale = 6)
	private BigDecimal rate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Source source;

	// Ver Receivable.createdAt: sem @CreationTimestamp, o Hibernate manda NULL
	// explícito no INSERT e o DEFAULT now() do Postgres nunca é acionado.
	@CreationTimestamp
	@Column(name = "valid_from", nullable = false)
	private OffsetDateTime validFrom;

	protected ExchangeRate() {
	}

	public ExchangeRate(Currency baseCurrency, Currency quoteCurrency, BigDecimal rate, Source source) {
		this.baseCurrency = baseCurrency;
		this.quoteCurrency = quoteCurrency;
		this.rate = rate;
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public Currency getBaseCurrency() {
		return baseCurrency;
	}

	public Currency getQuoteCurrency() {
		return quoteCurrency;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public Source getSource() {
		return source;
	}

	public OffsetDateTime getValidFrom() {
		return validFrom;
	}
}
