package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.ExchangeRateNotFoundException;
import com.srmasset.creditengine.domain.exception.ExchangeRateProviderUnavailableException;
import com.srmasset.creditengine.domain.fx.ExchangeRateProvider;
import com.srmasset.creditengine.domain.math.MathConstants;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import com.srmasset.creditengine.persistence.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A tabela exchange_rates só guarda uma direção por par cotado (ver seed: apenas USD->BRL). Um
 * motor multimoeda de verdade precisa liquidar nos dois sentidos, então aqui tentamos o par direto
 * e, na ausência, o par inverso (invertendo a taxa) antes de desistir.
 *
 * <p>Quando o provedor externo está habilitado, {@link #getCurrentRate} primeiro busca uma cotação
 * fresca ({@link ExchangeRateProvider}, protegido por retry/circuit breaker) e a persiste como uma
 * nova linha append-only (fonte {@link ExchangeRate.Source#MOCK_PROVIDER}, ver ADR 0003). Se o
 * provedor estiver indisponível (retries esgotados ou circuito aberto), cai para a última taxa
 * conhecida no banco — a liquidação nunca é bloqueada pela indisponibilidade de um provedor
 * externo.
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateProvider exchangeRateProvider;
    private final boolean providerEnabled;
    private final ExchangeRateMetricsRecorder exchangeRateMetricsRecorder;

    public ExchangeRateService(
            ExchangeRateRepository exchangeRateRepository,
            ExchangeRateProvider exchangeRateProvider,
            @Value("${app.exchange-rate-provider.enabled:true}") boolean providerEnabled,
            ExchangeRateMetricsRecorder exchangeRateMetricsRecorder) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateProvider = exchangeRateProvider;
        this.providerEnabled = providerEnabled;
        this.exchangeRateMetricsRecorder = exchangeRateMetricsRecorder;
    }

    @Transactional
    public BigDecimal getCurrentRate(Currency base, Currency quote) {
        if (providerEnabled) {
            try {
                BigDecimal freshRate =
                        exchangeRateProvider.fetchRate(base.getCode(), quote.getCode());
                exchangeRateRepository.save(
                        new ExchangeRate(
                                base, quote, freshRate, ExchangeRate.Source.MOCK_PROVIDER));
                exchangeRateMetricsRecorder.recordOutcome("success");
                return freshRate;
            } catch (ExchangeRateProviderUnavailableException e) {
                log.warn(
                        "FX provider unavailable for {}->{}, falling back to latest stored rate",
                        base.getCode(),
                        quote.getCode(),
                        e);
                exchangeRateMetricsRecorder.recordOutcome("fallback");
            }
        }
        return findLatestStoredRate(base, quote);
    }

    private BigDecimal findLatestStoredRate(Currency base, Currency quote) {
        return exchangeRateRepository
                .findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(base, quote)
                .map(ExchangeRate::getRate)
                .or(
                        () ->
                                exchangeRateRepository
                                        .findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
                                                quote, base)
                                        .map(
                                                inverse ->
                                                        BigDecimal.ONE.divide(
                                                                inverse.getRate(),
                                                                MathConstants.PRICING_CONTEXT)))
                .orElseThrow(() -> new ExchangeRateNotFoundException(base, quote));
    }
}
