package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.ExchangeRateNotFoundException;
import com.srmasset.creditengine.domain.math.MathConstants;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import com.srmasset.creditengine.persistence.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A tabela exchange_rates só guarda uma direção por par cotado (ver seed:
 * apenas USD->BRL). Um motor multimoeda de verdade precisa liquidar nos dois
 * sentidos, então aqui tentamos o par direto e, na ausência, o par inverso
 * (invertendo a taxa) antes de desistir.
 */
@Service
public class ExchangeRateService {

	private final ExchangeRateRepository exchangeRateRepository;

	public ExchangeRateService(ExchangeRateRepository exchangeRateRepository) {
		this.exchangeRateRepository = exchangeRateRepository;
	}

	@Transactional(readOnly = true)
	public BigDecimal findLatestRate(Currency base, Currency quote) {
		return exchangeRateRepository
				.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(base, quote)
				.map(ExchangeRate::getRate)
				.or(() -> exchangeRateRepository
						.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(quote, base)
						.map(inverse -> BigDecimal.ONE.divide(inverse.getRate(), MathConstants.PRICING_CONTEXT)))
				.orElseThrow(() -> new ExchangeRateNotFoundException(base, quote));
	}
}
