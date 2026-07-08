package com.srmasset.creditengine.domain.pricing;

import org.springframework.stereotype.Component;

@Component
public class ChequePreDatadoStrategy extends AbstractCompoundInterestPricingStrategy {

    @Override
    public ReceivableTypeCode getType() {
        return ReceivableTypeCode.CHEQUE_PRE_DATADO;
    }
}
