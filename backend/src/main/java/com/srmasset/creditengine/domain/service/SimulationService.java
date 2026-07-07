package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.math.MathConstants;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.pricing.PricingStrategy;
import com.srmasset.creditengine.domain.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.domain.pricing.TermCalculator;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fonte única de verdade da fórmula de precificação — o frontend chama o
 * endpoint apoiado neste serviço em vez de replicar o cálculo em JS/TS.
 */
@Service
public class SimulationService {

	private final ReceivableRepository receivableRepository;
	private final PricingStrategyResolver pricingStrategyResolver;

	public SimulationService(ReceivableRepository receivableRepository, PricingStrategyResolver pricingStrategyResolver) {
		this.receivableRepository = receivableRepository;
		this.pricingStrategyResolver = pricingStrategyResolver;
	}

	@Transactional(readOnly = true)
	public SimulationResult simulate(Long receivableId, BaseRate baseRate) {
		Receivable receivable = receivableRepository.findById(receivableId)
				.orElseThrow(() -> new ReceivableNotFoundException(receivableId));

		PricingStrategy strategy = pricingStrategyResolver.resolve(receivable.getReceivableType().getCode());
		BigDecimal presentValue = strategy.calculatePresentValue(receivable, baseRate);
		BigDecimal termInMonths = TermCalculator.monthsBetween(
				baseRate.referenceDate(), receivable.getDueDate(), MathConstants.PRICING_CONTEXT);

		return new SimulationResult(
				receivable.getId(),
				receivable.getFaceValue().setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
				presentValue.setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
				termInMonths.setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
				receivable.getFaceValueCurrency().getCode());
	}

	public record SimulationResult(
			Long receivableId,
			BigDecimal faceValue,
			BigDecimal presentValue,
			BigDecimal termInMonths,
			String currencyCode) {
	}
}
