package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.exception.CurrencyNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotPendingException;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.pricing.DuplicataMercantilStrategy;
import com.srmasset.creditengine.domain.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.entity.Settlement;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.SettlementRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private ReceivableRepository receivableRepository;

    @Mock private CurrencyRepository currencyRepository;

    @Mock private SettlementRepository settlementRepository;

    @Mock private ExchangeRateService exchangeRateService;

    private SettlementService settlementService;

    private final Currency brl = new Currency("BRL", "Real Brasileiro");
    private final Currency usd = new Currency("USD", "Dolar Americano");
    private final ReceivableType duplicata =
            new ReceivableType(
                    ReceivableTypeCode.DUPLICATA_MERCANTIL,
                    "Duplicata Mercantil",
                    new BigDecimal("1.5"));

    @BeforeEach
    void setUp() {
        PricingStrategyResolver pricingStrategyResolver =
                new PricingStrategyResolver(List.of(new DuplicataMercantilStrategy()));
        SettlementMetricsRecorder settlementMetricsRecorder =
                new SettlementMetricsRecorder(new SimpleMeterRegistry());
        settlementService =
                new SettlementService(
                        receivableRepository,
                        currencyRepository,
                        settlementRepository,
                        pricingStrategyResolver,
                        exchangeRateService,
                        settlementMetricsRecorder);
    }

    private Receivable newReceivable(Currency faceCurrency, LocalDate referenceDate) {
        Assignor assignor = new Assignor("Empresa Teste", "12345678000199");
        return new Receivable(
                assignor,
                duplicata,
                faceCurrency,
                new BigDecimal("10000.00"),
                "DOC-1",
                referenceDate,
                referenceDate.plusDays(30));
    }

    @Test
    void settlesWithoutConversionWhenSameCurrency() {
        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        Receivable receivable = newReceivable(brl, referenceDate);
        when(receivableRepository.findById(1L)).thenReturn(Optional.of(receivable));
        when(currencyRepository.findByCode("BRL")).thenReturn(Optional.of(brl));
        when(settlementRepository.saveAndFlush(any(Settlement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(receivableRepository.saveAndFlush(receivable)).thenReturn(receivable);

        Settlement settlement =
                settlementService.settle(
                        1L, "BRL", new BaseRate(new BigDecimal("2.0"), referenceDate));

        assertThat(settlement.getFxRateUsed()).isNull();
        assertThat(settlement.getNetValuePaymentCurrency())
                .isEqualByComparingTo(settlement.getPresentValueFaceCurrency());
        assertThat(receivable.getStatus()).isEqualTo(Receivable.Status.SETTLED);
    }

    @Test
    void convertsToPaymentCurrencyWhenDifferentFromFaceCurrency() {
        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        Receivable receivable = newReceivable(brl, referenceDate);
        when(receivableRepository.findById(1L)).thenReturn(Optional.of(receivable));
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usd));
        when(exchangeRateService.getCurrentRate(brl, usd)).thenReturn(new BigDecimal("0.185185"));
        when(settlementRepository.saveAndFlush(any(Settlement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(receivableRepository.saveAndFlush(receivable)).thenReturn(receivable);

        Settlement settlement =
                settlementService.settle(
                        1L, "USD", new BaseRate(new BigDecimal("2.0"), referenceDate));

        assertThat(settlement.getFxRateUsed()).isEqualByComparingTo(new BigDecimal("0.185185"));
        BigDecimal roughlyExpectedNetValue =
                settlement.getPresentValueFaceCurrency().multiply(new BigDecimal("0.185185"));
        assertThat(settlement.getNetValuePaymentCurrency())
                .isCloseTo(roughlyExpectedNetValue, Percentage.withPercentage(0.01));
    }

    @Test
    void throwsWhenReceivableNotFound() {
        when(receivableRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                settlementService.settle(
                                        99L,
                                        "BRL",
                                        new BaseRate(new BigDecimal("2.0"), LocalDate.now())))
                .isInstanceOf(ReceivableNotFoundException.class);
    }

    @Test
    void throwsWhenPaymentCurrencyNotFound() {
        Receivable receivable = newReceivable(brl, LocalDate.now());
        when(receivableRepository.findById(1L)).thenReturn(Optional.of(receivable));
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                settlementService.settle(
                                        1L,
                                        "XYZ",
                                        new BaseRate(new BigDecimal("2.0"), LocalDate.now())))
                .isInstanceOf(CurrencyNotFoundException.class);
    }

    @Test
    void throwsAndWritesNothingWhenReceivableAlreadySettled() {
        LocalDate referenceDate = LocalDate.of(2026, 7, 7);
        Receivable receivable = newReceivable(brl, referenceDate);
        receivable.markAsSettled();
        when(receivableRepository.findById(1L)).thenReturn(Optional.of(receivable));
        when(currencyRepository.findByCode("BRL")).thenReturn(Optional.of(brl));

        assertThatThrownBy(
                        () ->
                                settlementService.settle(
                                        1L,
                                        "BRL",
                                        new BaseRate(new BigDecimal("2.0"), referenceDate)))
                .isInstanceOf(ReceivableNotPendingException.class);

        verify(settlementRepository, never()).saveAndFlush(any());
        verify(receivableRepository, never()).saveAndFlush(any());
    }
}
