package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.CurrencyNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.math.MathConstants;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.pricing.PricingStrategy;
import com.srmasset.creditengine.domain.pricing.PricingStrategyResolver;
import com.srmasset.creditengine.domain.pricing.TermCalculator;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.Settlement;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.SettlementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liquida um único recebível: calcula o valor presente (moeda de face), converte para a moeda de
 * pagamento quando necessário e persiste o {@link Settlement} como snapshot de auditoria de todas
 * as taxas usadas.
 *
 * <p>{@code REQUIRES_NEW} (em vez de {@code REQUIRED}) torna explícito que cada liquidação é uma
 * unidade atômica independente — propriedade da qual {@link SettlementBatchService} depende para
 * processar um lote item a item sem que a falha de um item afete os demais (ver ADR 0004).
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final ReceivableRepository receivableRepository;
    private final CurrencyRepository currencyRepository;
    private final SettlementRepository settlementRepository;
    private final PricingStrategyResolver pricingStrategyResolver;
    private final ExchangeRateService exchangeRateService;
    private final SettlementMetricsRecorder settlementMetricsRecorder;

    public SettlementService(
            ReceivableRepository receivableRepository,
            CurrencyRepository currencyRepository,
            SettlementRepository settlementRepository,
            PricingStrategyResolver pricingStrategyResolver,
            ExchangeRateService exchangeRateService,
            SettlementMetricsRecorder settlementMetricsRecorder) {
        this.receivableRepository = receivableRepository;
        this.currencyRepository = currencyRepository;
        this.settlementRepository = settlementRepository;
        this.pricingStrategyResolver = pricingStrategyResolver;
        this.exchangeRateService = exchangeRateService;
        this.settlementMetricsRecorder = settlementMetricsRecorder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Settlement settle(Long receivableId, String paymentCurrencyCode, BaseRate baseRate) {
        // Escopo de MDC por recebível: correlaciona os logs desta liquidação
        // específica, distinto do requestId (escopo por requisição HTTP) —
        // necessário porque uma liquidação em lote processa vários recebíveis
        // sequencialmente na mesma requisição/thread (ver ADR 0004 e 0007).
        MDC.put("receivableId", String.valueOf(receivableId));
        try {
            Receivable receivable =
                    receivableRepository
                            .findById(receivableId)
                            .orElseThrow(() -> new ReceivableNotFoundException(receivableId));
            Currency paymentCurrency =
                    currencyRepository
                            .findByCode(paymentCurrencyCode)
                            .orElseThrow(() -> new CurrencyNotFoundException(paymentCurrencyCode));

            // Falha rápido, sem nenhuma escrita, se o recebível já não está PENDING.
            receivable.markAsSettled();

            PricingStrategy strategy =
                    pricingStrategyResolver.resolve(receivable.getReceivableType().getCode());
            BigDecimal presentValueFaceCurrency =
                    strategy.calculatePresentValue(receivable, baseRate);
            long termDays =
                    TermCalculator.daysBetween(baseRate.referenceDate(), receivable.getDueDate());
            BigDecimal termMonths =
                    TermCalculator.monthsBetween(
                            baseRate.referenceDate(),
                            receivable.getDueDate(),
                            MathConstants.PRICING_CONTEXT);

            Currency faceCurrency = receivable.getFaceValueCurrency();
            BigDecimal fxRateUsed = null;
            BigDecimal netValuePaymentCurrency = presentValueFaceCurrency;
            if (!faceCurrency.getCode().equals(paymentCurrency.getCode())) {
                fxRateUsed = exchangeRateService.getCurrentRate(faceCurrency, paymentCurrency);
                netValuePaymentCurrency =
                        presentValueFaceCurrency.multiply(
                                fxRateUsed, MathConstants.PRICING_CONTEXT);
            }

            Settlement settlement =
                    new Settlement(
                            receivable,
                            paymentCurrency,
                            receivable.getFaceValue(),
                            faceCurrency,
                            baseRate.monthlyPercent()
                                    .setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            receivable
                                    .getReceivableType()
                                    .getSpreadPercentMonthly()
                                    .setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            Math.toIntExact(termDays),
                            termMonths.setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            presentValueFaceCurrency.setScale(
                                    MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            fxRateUsed == null
                                    ? null
                                    : fxRateUsed.setScale(
                                            MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            netValuePaymentCurrency.setScale(
                                    MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                            OffsetDateTime.now());

            // Ordem deliberada: se a liquidação duplicada e o conflito de versão
            // pudessem colidir na mesma corrida, a constraint UNIQUE (mais barata
            // de verificar) aparece primeiro que o optimistic lock.
            settlementRepository.saveAndFlush(settlement);
            receivableRepository.saveAndFlush(receivable);

            settlementMetricsRecorder.recordSettlement(
                    paymentCurrencyCode, netValuePaymentCurrency);

            log.info(
                    "Settlement {} concluída para o recebível {}: {} {} -> {} {}",
                    settlement.getId(),
                    receivableId,
                    settlement.getFaceValue(),
                    faceCurrency.getCode(),
                    netValuePaymentCurrency.setScale(
                            MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN),
                    paymentCurrencyCode);

            return settlement;
        } finally {
            MDC.remove("receivableId");
        }
    }
}
