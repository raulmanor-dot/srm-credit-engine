package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * Requisito de concorrência do nível Sênior: duas "sessões" concorrentes que
 * carregaram o mesmo recebível não podem liquidá-lo duas vezes. Cada
 * chamada de repositório aqui roda na sua própria transação (a classe de
 * teste não é @Transactional), então os dois `findById` abaixo produzem
 * duas instâncias JPA independentes com o mesmo `version` — simulando de
 * fato duas requisições HTTP concorrentes, não uma race condition fake
 * dentro do mesmo persistence context.
 */
class ReceivableOptimisticLockingIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private CurrencyRepository currencyRepository;

	@Autowired
	private ReceivableTypeRepository receivableTypeRepository;

	@Autowired
	private AssignorRepository assignorRepository;

	@Autowired
	private ReceivableRepository receivableRepository;

	@Test
	void secondConcurrentSettlementAttemptIsRejectedByOptimisticLock() {
		Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL)
				.orElseThrow();
		Assignor assignor = assignorRepository.save(new Assignor("Empresa Concorrencia", "22333444000155"));

		LocalDate today = LocalDate.now();
		Receivable saved = receivableRepository.save(new Receivable(
				assignor, duplicata, brl, new BigDecimal("1000.00"), "DOC-LOCK-1", today, today.plusDays(30)));
		Long id = saved.getId();

		// duas "sessões" concorrentes carregam o mesmo estado (version = 0)
		Receivable sessionA = receivableRepository.findById(id).orElseThrow();
		Receivable sessionB = receivableRepository.findById(id).orElseThrow();

		sessionA.markAsSettled();
		receivableRepository.saveAndFlush(sessionA);

		sessionB.markAsSettled();
		assertThatThrownBy(() -> receivableRepository.saveAndFlush(sessionB))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		Receivable reloaded = receivableRepository.findById(id).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(Receivable.Status.SETTLED);
	}
}
