package com.srmasset.creditengine.mockprovider;

import com.srmasset.creditengine.domain.math.MathConstants;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gera uma cotação "fresca" para um par de moedas a partir de uma tabela de taxas base configurada
 * (ver {@code app.mock-provider.rates}), aplicando um jitter aleatório para simular a variação
 * natural de um provedor real. Aceita também o par invertido (mesma lógica de {@code
 * ExchangeRateService}), já que só é prático configurar uma direção por par.
 */
@Component
public class MockRateGenerator {

    private final MockProviderProperties properties;
    private final Random random;

    @Autowired
    public MockRateGenerator(MockProviderProperties properties) {
        this(properties, new Random());
    }

    MockRateGenerator(MockProviderProperties properties, Random random) {
        this.properties = properties;
        this.random = random;
    }

    public Optional<BigDecimal> generate(String baseCode, String quoteCode) {
        BigDecimal direct =
                properties.rates() == null
                        ? null
                        : properties.rates().get(key(baseCode, quoteCode));
        if (direct != null) {
            return Optional.of(applyJitter(direct));
        }
        BigDecimal inverse =
                properties.rates() == null
                        ? null
                        : properties.rates().get(key(quoteCode, baseCode));
        if (inverse != null) {
            BigDecimal inverted = BigDecimal.ONE.divide(inverse, MathConstants.PRICING_CONTEXT);
            return Optional.of(applyJitter(inverted));
        }
        return Optional.empty();
    }

    private BigDecimal applyJitter(BigDecimal baseRate) {
        double jitterFraction = (random.nextDouble() * 2 - 1) * properties.jitterPercent() / 100.0;
        BigDecimal factor =
                BigDecimal.ONE.add(
                        BigDecimal.valueOf(jitterFraction), MathConstants.PRICING_CONTEXT);
        return baseRate.multiply(factor, MathConstants.PRICING_CONTEXT)
                .setScale(MathConstants.MONEY_SCALE, RoundingMode.HALF_EVEN);
    }

    private static String key(String base, String quote) {
        return base + "-" + quote;
    }
}
