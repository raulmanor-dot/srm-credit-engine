package com.srmasset.creditengine.domain.pricing;

import org.springframework.stereotype.Component;

@Component
public class DuplicataMercantilStrategy extends AbstractCompoundInterestPricingStrategy {

	@Override
	public ReceivableTypeCode getType() {
		return ReceivableTypeCode.DUPLICATA_MERCANTIL;
	}
}
