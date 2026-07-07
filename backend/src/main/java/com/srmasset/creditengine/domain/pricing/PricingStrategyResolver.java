package com.srmasset.creditengine.domain.pricing;

import com.srmasset.creditengine.domain.exception.UnsupportedReceivableTypeException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Registro tipo -> bean via injeção de {@code List<PricingStrategy>} pelo
 * Spring (todo bean {@link PricingStrategy} do contexto entra no mapa
 * automaticamente). Adicionar um novo tipo de recebível é criar uma nova
 * classe {@code @Component} implementando {@link PricingStrategy}; este
 * resolver não precisa mudar — Open/Closed em vez de if/else por tipo.
 */
@Component
public class PricingStrategyResolver {

	private final Map<ReceivableTypeCode, PricingStrategy> strategiesByType;

	public PricingStrategyResolver(List<PricingStrategy> strategies) {
		this.strategiesByType = strategies.stream()
				.collect(Collectors.toMap(PricingStrategy::getType, Function.identity()));
	}

	public PricingStrategy resolve(ReceivableTypeCode type) {
		PricingStrategy strategy = strategiesByType.get(type);
		if (strategy == null) {
			throw new UnsupportedReceivableTypeException(type);
		}
		return strategy;
	}
}
