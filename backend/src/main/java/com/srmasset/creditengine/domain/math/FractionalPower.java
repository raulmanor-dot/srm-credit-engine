package com.srmasset.creditengine.domain.math;

import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * BigDecimal.pow(int) só aceita expoente inteiro, mas o prazo em meses aqui é fracionário (dias
 * corridos / 30). Math.pow(double, double) resolveria, mas introduz erro de arredondamento binário
 * inaceitável em cálculo financeiro auditável. Por isso delegamos para big-math, que implementa
 * exponenciação fracionária (via exp(y * ln(x))) com precisão arbitrária controlada por
 * MathContext.
 */
public final class FractionalPower {

    private FractionalPower() {}

    public static BigDecimal pow(BigDecimal base, BigDecimal exponent, MathContext mc) {
        return BigDecimalMath.pow(base, exponent, mc);
    }
}
