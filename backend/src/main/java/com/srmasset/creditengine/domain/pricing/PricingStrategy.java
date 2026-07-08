package com.srmasset.creditengine.domain.pricing;

import com.srmasset.creditengine.persistence.entity.Receivable;
import java.math.BigDecimal;

public interface PricingStrategy {

    ReceivableTypeCode getType();

    BigDecimal calculatePresentValue(Receivable receivable, BaseRate baseRate);
}
