package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.entity.Settlement;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import com.srmasset.creditengine.persistence.repository.SettlementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Defesa em profundidade (ver ADR 0004): mesmo que o optimistic lock de
 * `receivables` não pegue uma corrida, o banco rejeita fisicamente uma
 * segunda linha em `settlements` para o mesmo `receivable_id`.
 */
class SettlementUniqueConstraintIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private CurrencyRepository currencyRepository;

	@Autowired
	private ReceivableTypeRepository receivableTypeRepository;

	@Autowired
	private AssignorRepository assignorRepository;

	@Autowired
	private ReceivableRepository receivableRepository;

	@Autowired
	private SettlementRepository settlementRepository;

	@Test
	void secondSettlementForSameReceivableViolatesUniqueConstraint() {
		Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL)
				.orElseThrow();
		Assignor assignor = assignorRepository.save(new Assignor("Empresa Unique", "33444555000166"));

		LocalDate today = LocalDate.now();
		Receivable receivable = receivableRepository.save(new Receivable(
				assignor, duplicata, brl, new BigDecimal("1000.00"), "DOC-UNQ-1", today, today.plusDays(30)));

		Settlement first = newSettlement(receivable, brl);
		settlementRepository.saveAndFlush(first);

		Settlement second = newSettlement(receivable, brl);
		assertThatThrownBy(() -> settlementRepository.saveAndFlush(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Settlement newSettlement(Receivable receivable, Currency currency) {
		return new Settlement(
				receivable,
				currency,
				receivable.getFaceValue(),
				currency,
				new BigDecimal("2.000000"),
				new BigDecimal("1.500000"),
				30,
				new BigDecimal("1.000000"),
				new BigDecimal("965.250965"),
				null,
				new BigDecimal("965.250965"),
				OffsetDateTime.now());
	}
}
