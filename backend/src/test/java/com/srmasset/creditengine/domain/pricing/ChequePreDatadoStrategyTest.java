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

class ChequePreDatadoStrategyTest {

    private final ChequePreDatadoStrategy strategy = new ChequePreDatadoStrategy();

    @Test
    void identifiesItsReceivableType() {
        assertThat(strategy.getType()).isEqualTo(ReceivableTypeCode.CHEQUE_PRE_DATADO);
    }

    @Test
    void discountsFaceValueByBaseRatePlusSpreadOverFractionalTerm() {
        ReceivableType type =
                new ReceivableType(
                        ReceivableTypeCode.CHEQUE_PRE_DATADO,
                        "Cheque Pre-datado",
                        new BigDecimal("2.5"));
        Currency brl = new Currency("BRL", "Real Brasileiro");
        Assignor assignor = new Assignor("Empresa Y", "98765432000188");

        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        LocalDate dueDate = referenceDate.plusDays(45);
        Receivable receivable =
                new Receivable(
                        assignor,
                        type,
                        brl,
                        new BigDecimal("5000.00"),
                        "CHQ-1",
                        referenceDate,
                        dueDate);

        BaseRate baseRate = new BaseRate(new BigDecimal("1.0"), referenceDate);

        BigDecimal presentValue = strategy.calculatePresentValue(receivable, baseRate);

        // taxa total = 1,0% (base) + 2,5% (spread) = 3,5% a.m.; prazo = 45/30 = 1,5 mes
        // PV = 5000 / 1,035^1.5
        BigDecimal onePlusI = new BigDecimal("1.035");
        double expectedDouble = 5000.0 / Math.pow(onePlusI.doubleValue(), 1.5);
        BigDecimal expected =
                BigDecimal.valueOf(expectedDouble).setScale(2, RoundingMode.HALF_EVEN);

        assertThat(presentValue.setScale(2, RoundingMode.HALF_EVEN)).isEqualByComparingTo(expected);

        // Guarda de sanidade financeira: com taxa total positiva, o valor presente
        // NUNCA pode superar o valor de face — ver docs/crisis-simulation.md.
        assertThat(presentValue).isLessThan(receivable.getFaceValue());
    }
}
