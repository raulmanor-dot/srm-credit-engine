package com.srmasset.creditengine.mockprovider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MockRateResponse(String base, String quote, BigDecimal rate, OffsetDateTime timestamp) {
}
