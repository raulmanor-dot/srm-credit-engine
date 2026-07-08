package com.srmasset.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Taxa base de mercado (a.m., em percentual — ex.: 2.0 significa 2%) e a data de referência usada
 * para contar o prazo até o vencimento. Value object imutável: evita que a Strategy chame
 * LocalDate.now() internamente, o que quebraria a determinismo e a testabilidade do cálculo.
 */
public record BaseRate(BigDecimal monthlyPercent, LocalDate referenceDate) {}
