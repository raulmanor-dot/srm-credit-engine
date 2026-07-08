package com.srmasset.creditengine.persistence.entity;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Representa a linha configurável em receivable_types. O spread mensal vem do banco (não é
 * constante Java) para permitir ajuste operacional sem deploy.
 */
@Entity
@Table(name = "receivable_types")
public class ReceivableType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private ReceivableTypeCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "spread_percent_monthly", nullable = false, precision = 7, scale = 4)
    private BigDecimal spreadPercentMonthly;

    @Column(nullable = false)
    private boolean active = true;

    protected ReceivableType() {}

    public ReceivableType(ReceivableTypeCode code, String name, BigDecimal spreadPercentMonthly) {
        this.code = code;
        this.name = name;
        this.spreadPercentMonthly = spreadPercentMonthly;
    }

    public Long getId() {
        return id;
    }

    public ReceivableTypeCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSpreadPercentMonthly() {
        return spreadPercentMonthly;
    }

    public boolean isActive() {
        return active;
    }
}
