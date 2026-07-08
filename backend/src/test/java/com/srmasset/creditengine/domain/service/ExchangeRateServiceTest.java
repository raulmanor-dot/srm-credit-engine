package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.exception.ExchangeRateNotFoundException;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import com.srmasset.creditengine.persistence.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	private final Currency usd = new Currency("USD", "Dolar Americano");
	private final Currency brl = new Currency("BRL", "Real Brasileiro");

	@Test
	void returnsDirectRateWhenPairExistsInThatDirection() {
		ExchangeRateService service = new ExchangeRateService(exchangeRateRepository);
		ExchangeRate rate = new ExchangeRate(usd, brl, new BigDecimal("5.400000"), ExchangeRate.Source.MANUAL);
		when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(usd, brl))
				.thenReturn(Optional.of(rate));

		BigDecimal result = service.findLatestRate(usd, brl);

		assertThat(result).isEqualByComparingTo(new BigDecimal("5.400000"));
	}

	@Test
	void invertsRateWhenOnlyInversePairExists() {
		ExchangeRateService service = new ExchangeRateService(exchangeRateRepository);
		ExchangeRate rate = new ExchangeRate(usd, brl, new BigDecimal("5.400000"), ExchangeRate.Source.MANUAL);
		when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(brl, usd))
				.thenReturn(Optional.empty());
		when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(usd, brl))
				.thenReturn(Optional.of(rate));

		BigDecimal result = service.findLatestRate(brl, usd);

		BigDecimal expected = BigDecimal.ONE.divide(new BigDecimal("5.400000"), 6, RoundingMode.HALF_EVEN);
		assertThat(result.setScale(6, RoundingMode.HALF_EVEN)).isEqualByComparingTo(expected);
	}

	@Test
	void throwsWhenNeitherDirectionExists() {
		ExchangeRateService service = new ExchangeRateService(exchangeRateRepository);
		when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(usd, brl))
				.thenReturn(Optional.empty());
		when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(brl, usd))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.findLatestRate(usd, brl))
				.isInstanceOf(ExchangeRateNotFoundException.class);
	}
}
