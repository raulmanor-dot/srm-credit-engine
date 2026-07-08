package com.srmasset.creditengine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Registro de auditoria da liquidação: guarda snapshot de todas as taxas usadas (base, spread,
 * câmbio) além dos valores em ambas as moedas — quem audita precisa saber exatamente qual taxa foi
 * aplicada, não só o resultado. A UNIQUE em receivable_id (ver V6) impede fisicamente uma segunda
 * liquidação do mesmo título.
 */
@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "receivable_id", unique = true)
    private Receivable receivable;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_currency_id")
    private Currency paymentCurrency;

    @Column(name = "face_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal faceValue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "face_value_currency_id")
    private Currency faceValueCurrency;

    @Column(name = "base_rate_used", nullable = false, precision = 9, scale = 6)
    private BigDecimal baseRateUsed;

    @Column(name = "spread_used", nullable = false, precision = 9, scale = 6)
    private BigDecimal spreadUsed;

    @Column(name = "term_days", nullable = false)
    private Integer termDays;

    @Column(name = "term_months", nullable = false, precision = 12, scale = 6)
    private BigDecimal termMonths;

    @Column(name = "present_value_face_currency", nullable = false, precision = 19, scale = 6)
    private BigDecimal presentValueFaceCurrency;

    @Column(name = "fx_rate_used", precision = 19, scale = 6)
    private BigDecimal fxRateUsed;

    @Column(name = "net_value_payment_currency", nullable = false, precision = 19, scale = 6)
    private BigDecimal netValuePaymentCurrency;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "settled_at", nullable = false)
    private OffsetDateTime settledAt;

    // Ver Receivable.createdAt: sem @CreationTimestamp, o Hibernate manda NULL
    // explícito no INSERT e o DEFAULT now() do Postgres nunca é acionado.
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Settlement() {}

    public Settlement(
            Receivable receivable,
            Currency paymentCurrency,
            BigDecimal faceValue,
            Currency faceValueCurrency,
            BigDecimal baseRateUsed,
            BigDecimal spreadUsed,
            Integer termDays,
            BigDecimal termMonths,
            BigDecimal presentValueFaceCurrency,
            BigDecimal fxRateUsed,
            BigDecimal netValuePaymentCurrency,
            OffsetDateTime settledAt) {
        this.receivable = receivable;
        this.paymentCurrency = paymentCurrency;
        this.faceValue = faceValue;
        this.faceValueCurrency = faceValueCurrency;
        this.baseRateUsed = baseRateUsed;
        this.spreadUsed = spreadUsed;
        this.termDays = termDays;
        this.termMonths = termMonths;
        this.presentValueFaceCurrency = presentValueFaceCurrency;
        this.fxRateUsed = fxRateUsed;
        this.netValuePaymentCurrency = netValuePaymentCurrency;
        this.settledAt = settledAt;
    }

    public Long getId() {
        return id;
    }

    public Receivable getReceivable() {
        return receivable;
    }

    public Currency getPaymentCurrency() {
        return paymentCurrency;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public Currency getFaceValueCurrency() {
        return faceValueCurrency;
    }

    public BigDecimal getBaseRateUsed() {
        return baseRateUsed;
    }

    public BigDecimal getSpreadUsed() {
        return spreadUsed;
    }

    public Integer getTermDays() {
        return termDays;
    }

    public BigDecimal getTermMonths() {
        return termMonths;
    }

    public BigDecimal getPresentValueFaceCurrency() {
        return presentValueFaceCurrency;
    }

    public BigDecimal getFxRateUsed() {
        return fxRateUsed;
    }

    public BigDecimal getNetValuePaymentCurrency() {
        return netValuePaymentCurrency;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getSettledAt() {
        return settledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
