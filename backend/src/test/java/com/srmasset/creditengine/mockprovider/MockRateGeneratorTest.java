package com.srmasset.creditengine.mockprovider;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MockRateGeneratorTest {

    @Test
    void staysWithinConfiguredJitterPercent() {
        MockProviderProperties properties =
                new MockProviderProperties(
                        0.2, 0, 0, 2.0, Map.of("USD-BRL", new BigDecimal("5.400000")));
        MockRateGenerator generator = new MockRateGenerator(properties, new Random(42));

        Optional<BigDecimal> result = generator.generate("USD", "BRL");

        assertThat(result).isPresent();
        BigDecimal min = new BigDecimal("5.292000");
        BigDecimal max = new BigDecimal("5.508000");
        assertThat(result.get()).isBetween(min, max);
    }

    @Test
    void invertsRateWhenOnlyInversePairConfigured() {
        MockProviderProperties properties =
                new MockProviderProperties(
                        0.0, 0, 0, 0.0, Map.of("USD-BRL", new BigDecimal("5.400000")));
        MockRateGenerator generator = new MockRateGenerator(properties, new Random(1));

        Optional<BigDecimal> result = generator.generate("BRL", "USD");

        BigDecimal expected =
                BigDecimal.ONE.divide(new BigDecimal("5.400000"), 6, RoundingMode.HALF_EVEN);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(expected);
    }

    @Test
    void returnsEmptyForUnknownPair() {
        MockProviderProperties properties = new MockProviderProperties(0.0, 0, 0, 0.0, Map.of());
        MockRateGenerator generator = new MockRateGenerator(properties, new Random(1));

        assertThat(generator.generate("EUR", "JPY")).isEmpty();
    }
}
