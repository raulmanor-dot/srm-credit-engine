package com.srmasset.creditengine.domain.math;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Precisão de 20 dígitos com HALF_EVEN (arredondamento bancário) para todo o
 * cálculo interno de precificação. O corte para NUMERIC(19,6) só acontece na
 * borda (persistência/response), nunca nas etapas intermediárias.
 */
public final class MathConstants {

	private MathConstants() {
	}

	public static final MathContext PRICING_CONTEXT = new MathContext(20, RoundingMode.HALF_EVEN);

	public static final int MONEY_SCALE = 6;
}
