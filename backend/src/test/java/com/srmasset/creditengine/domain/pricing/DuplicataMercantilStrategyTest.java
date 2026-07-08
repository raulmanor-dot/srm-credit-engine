package com.srmasset.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DuplicataMercantilStrategyTest {

    private final DuplicataMercantilStrategy strategy = new DuplicataMercantilStrategy();

    @Test
    void identifiesItsReceivableType() {
        assertThat(strategy.getType()).isEqualTo(ReceivableTypeCode.DUPLICATA_MERCANTIL);
    }

    @Test
    void discountsFaceValueByBaseRatePlusSpreadOverOneMonth() {
        ReceivableType type =
                new ReceivableType(
                        ReceivableTypeCode.DUPLICATA_MERCANTIL,
                        "Duplicata Mercantil",
                        new BigDecimal("1.5"));
        Currency brl = new Currency("BRL", "Real Brasileiro");
        Assignor assignor = new Assignor("Empresa X", "12345678000199");

        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        LocalDate dueDate = referenceDate.plusDays(30);
        Receivable receivable =
                new Receivable(
                        assignor,
                        type,
                        brl,
                        new BigDecimal("10000.00"),
                        "DOC-1",
                        referenceDate,
                        dueDate);

        BaseRate baseRate = new BaseRate(new BigDecimal("2.0"), referenceDate);

        BigDecimal presentValue = strategy.calculatePresentValue(receivable, baseRate);

        // taxa total = 2,0% (base) - 1,5% (spread) = 0,5% a.m.; prazo = 30/30 = 1 mes
        // PV = 10000 / 1,005
        BigDecimal expected =
                new BigDecimal("10000.00")
                        .divide(new BigDecimal("1.005"), 6, RoundingMode.HALF_EVEN);

        assertThat(presentValue.setScale(6, RoundingMode.HALF_EVEN)).isEqualByComparingTo(expected);
    }

    @Test
    void presentValueEqualsFaceValueWhenTermIsZero() {
        ReceivableType type =
                new ReceivableType(
                        ReceivableTypeCode.DUPLICATA_MERCANTIL,
                        "Duplicata Mercantil",
                        new BigDecimal("1.5"));
        Currency brl = new Currency("BRL", "Real Brasileiro");
        Assignor assignor = new Assignor("Empresa X", "12345678000199");

        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        Receivable receivable =
                new Receivable(
                        assignor,
                        type,
                        brl,
                        new BigDecimal("10000.00"),
                        "DOC-1",
                        referenceDate,
                        referenceDate);

        BaseRate baseRate = new BaseRate(new BigDecimal("2.0"), referenceDate);

        BigDecimal presentValue = strategy.calculatePresentValue(receivable, baseRate);

        assertThat(presentValue.setScale(2, RoundingMode.HALF_EVEN))
                .isEqualByComparingTo(new BigDecimal("10000.00"));
    }
}
