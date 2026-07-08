package com.srmasset.creditengine.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Taxa base, moeda de pagamento e data de referência são compartilhadas por
 * todo o lote (o operador liquida N recebíveis "contra a taxa de mercado de
 * agora, pagos em USD"). Não há uma taxa por item: assim como em
 * {@link SimulationRequest}, a taxa base é sempre um input pontual informado
 * pelo operador, não um valor persistido por recebível.
 */
public record BatchSettlementRequest(
		@NotEmpty List<@NotNull Long> receivableIds,
		@NotBlank String paymentCurrencyCode,
		@NotNull @Positive BigDecimal baseRateMonthlyPercent,
		LocalDate referenceDate) {
}
