package com.srmasset.creditengine.application.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SettlementResponse(
        Long settlementId,
        Long receivableId,
        BigDecimal faceValue,
        String faceValueCurrencyCode,
        BigDecimal presentValueFaceCurrency,
        BigDecimal netValuePaymentCurrency,
        String paymentCurrencyCode,
        BigDecimal fxRateUsed,
        BigDecimal baseRateUsed,
        BigDecimal spreadUsed,
        Integer termDays,
        BigDecimal termMonths,
        OffsetDateTime settledAt) {}
