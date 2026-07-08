package com.srmasset.creditengine.application.dto;

import com.srmasset.creditengine.persistence.entity.Currency;
import java.time.OffsetDateTime;

public record CurrencyResponse(
        Long id, String code, String name, boolean active, OffsetDateTime createdAt) {

    public static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(
                currency.getId(),
                currency.getCode(),
                currency.getName(),
                currency.isActive(),
                currency.getCreatedAt());
    }
}
