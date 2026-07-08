package com.srmasset.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Convenção de contagem de prazo, explícita para eliminar a ambiguidade
 * clássica de fórmulas com taxa mensal: dias corridos entre a data de
 * referência e o vencimento, divididos por 30 (mês comercial de 30 dias).
 * Não é dias úteis, não é a convenção bancária 30/360 completa — é a divisão
 * simples dias/30, suficiente para o escopo deste motor e documentada aqui.
 */
public final class TermCalculator {

	private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);

	private TermCalculator() {
	}

	public static long daysBetween(LocalDate referenceDate, LocalDate dueDate) {
		long days = ChronoUnit.DAYS.between(referenceDate, dueDate);
		if (days < 0) {
			throw new IllegalArgumentException("dueDate must not be before referenceDate");
		}
		return days;
	}

	public static BigDecimal monthsBetween(LocalDate referenceDate, LocalDate dueDate, MathContext mc) {
		long days = daysBetween(referenceDate, dueDate);
		return BigDecimal.valueOf(days).divide(DAYS_PER_MONTH, mc);
	}
}
