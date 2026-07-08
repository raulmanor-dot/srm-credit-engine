package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.exception.ExchangeRateNotFoundException;
import com.srmasset.creditengine.domain.exception.ExchangeRateProviderUnavailableException;
import com.srmasset.creditengine.domain.fx.ExchangeRateProvider;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import com.srmasset.creditengine.persistence.repository.ExchangeRateRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock private ExchangeRateRepository exchangeRateRepository;

    @Mock private ExchangeRateProvider exchangeRateProvider;

    private final ExchangeRateMetricsRecorder exchangeRateMetricsRecorder =
            new ExchangeRateMetricsRecorder(new SimpleMeterRegistry());

    private final Currency usd = new Currency("USD", "Dolar Americano");
    private final Currency brl = new Currency("BRL", "Real Brasileiro");

    @Test
    void returnsDirectRateWhenPairExistsInThatDirection() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        false,
                        exchangeRateMetricsRecorder);
        ExchangeRate rate =
                new ExchangeRate(usd, brl, new BigDecimal("5.400000"), ExchangeRate.Source.MANUAL);
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        usd, brl))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.getCurrentRate(usd, brl);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5.400000"));
        verifyNoInteractions(exchangeRateProvider);
    }

    @Test
    void invertsRateWhenOnlyInversePairExists() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        false,
                        exchangeRateMetricsRecorder);
        ExchangeRate rate =
                new ExchangeRate(usd, brl, new BigDecimal("5.400000"), ExchangeRate.Source.MANUAL);
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        brl, usd))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        usd, brl))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.getCurrentRate(brl, usd);

        BigDecimal expected =
                BigDecimal.ONE.divide(new BigDecimal("5.400000"), 6, RoundingMode.HALF_EVEN);
        assertThat(result.setScale(6, RoundingMode.HALF_EVEN)).isEqualByComparingTo(expected);
    }

    @Test
    void throwsWhenNeitherDirectionExistsAndProviderDisabled() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        false,
                        exchangeRateMetricsRecorder);
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        usd, brl))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        brl, usd))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentRate(usd, brl))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }

    @Test
    void persistsProviderRateAsMockProviderRowAndReturnsIt() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        true,
                        exchangeRateMetricsRecorder);
        when(exchangeRateProvider.fetchRate("USD", "BRL")).thenReturn(new BigDecimal("5.500000"));

        BigDecimal result = service.getCurrentRate(usd, brl);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5.500000"));
        verify(exchangeRateRepository).save(any(ExchangeRate.class));
    }

    @Test
    void fallsBackToLatestStoredRateWhenProviderUnavailable() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        true,
                        exchangeRateMetricsRecorder);
        when(exchangeRateProvider.fetchRate("USD", "BRL"))
                .thenThrow(
                        new ExchangeRateProviderUnavailableException(
                                "USD", "BRL", new RuntimeException("boom")));
        ExchangeRate storedRate =
                new ExchangeRate(usd, brl, new BigDecimal("5.400000"), ExchangeRate.Source.MANUAL);
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        usd, brl))
                .thenReturn(Optional.of(storedRate));

        BigDecimal result = service.getCurrentRate(usd, brl);

        assertThat(result).isEqualByComparingTo(new BigDecimal("5.400000"));
        verify(exchangeRateRepository, never()).save(any(ExchangeRate.class));
    }

    @Test
    void throwsExchangeRateNotFoundWhenProviderDownAndNoStoredRate() {
        ExchangeRateService service =
                new ExchangeRateService(
                        exchangeRateRepository,
                        exchangeRateProvider,
                        true,
                        exchangeRateMetricsRecorder);
        when(exchangeRateProvider.fetchRate("USD", "BRL"))
                .thenThrow(
                        new ExchangeRateProviderUnavailableException(
                                "USD", "BRL", new RuntimeException("boom")));
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        usd, brl))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                        brl, usd))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentRate(usd, brl))
                .isInstanceOf(ExchangeRateNotFoundException.class);
    }
}
