package com.srmasset.creditengine.infrastructure.fx;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProviderRateResponse(
        String base, String quote, BigDecimal rate, OffsetDateTime timestamp) {}
