package com.srmasset.creditengine.domain.pricing;

import com.srmasset.creditengine.domain.math.FractionalPower;
import com.srmasset.creditengine.domain.math.MathConstants;
import com.srmasset.creditengine.persistence.entity.Receivable;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Base comum aos tipos de recebível precificados por juros compostos: taxa mensal (base + spread do
 * tipo) elevada ao prazo fracionário em meses. O spread NÃO é constante Java — vem de {@code
 * receivable.getReceivableType().getSpreadPercentMonthly()}, ou seja, é configurável em banco
 * (tabela receivable_types) sem exigir deploy.
 *
 * <p>Um tipo futuro com fórmula distinta (ex.: juros simples) implementa {@link PricingStrategy}
 * diretamente, sem alterar esta classe nem o {@link PricingStrategyResolver} — Open/Closed na
 * prática.
 */
public abstract class AbstractCompoundInterestPricingStrategy implements PricingStrategy {

    private static final MathContext MC = MathConstants.PRICING_CONTEXT;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public BigDecimal calculatePresentValue(Receivable receivable, BaseRate baseRate) {
        BigDecimal spreadPercent = receivable.getReceivableType().getSpreadPercentMonthly();
        BigDecimal totalRatePercent = baseRate.monthlyPercent().subtract(spreadPercent, MC);
        BigDecimal monthlyRateDecimal = totalRatePercent.divide(ONE_HUNDRED, MC);

        BigDecimal termInMonths =
                TermCalculator.monthsBetween(baseRate.referenceDate(), receivable.getDueDate(), MC);

        BigDecimal onePlusI = BigDecimal.ONE.add(monthlyRateDecimal, MC);
        BigDecimal discountFactor = FractionalPower.pow(onePlusI, termInMonths, MC);

        return receivable.getFaceValue().divide(discountFactor, MC);
    }
}
