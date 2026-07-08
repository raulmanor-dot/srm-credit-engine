package com.srmasset.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srmasset.creditengine.domain.math.MathConstants;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TermCalculatorTest {

    @Test
    void thirtyDayTermIsExactlyOneMonth() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(30);

        BigDecimal months = TermCalculator.monthsBetween(from, to, MathConstants.PRICING_CONTEXT);

        assertThat(months).isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    void supportsFractionalTerms() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(45);

        BigDecimal months = TermCalculator.monthsBetween(from, to, MathConstants.PRICING_CONTEXT);

        assertThat(months).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    void rejectsDueDateBeforeReferenceDate() {
        LocalDate from = LocalDate.of(2026, 1, 10);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(
                        () -> TermCalculator.monthsBetween(from, to, MathConstants.PRICING_CONTEXT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
