package com.srmasset.creditengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.domain.service.SettlementService;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import com.srmasset.creditengine.persistence.report.SettlementReportRepository;
import com.srmasset.creditengine.persistence.report.SettlementStatementRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercita o caminho paralelo direto: SettlementReportRepository (SQL
 * nativo), sem passar por SettlementController/Service — provando que o
 * atalho funciona isoladamente contra dados reais persistidos.
 */
class ReportControllerIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private CurrencyRepository currencyRepository;

	@Autowired
	private ReceivableTypeRepository receivableTypeRepository;

	@Autowired
	private AssignorRepository assignorRepository;

	@Autowired
	private ReceivableRepository receivableRepository;

	@Autowired
	private SettlementService settlementService;

	@Autowired
	private SettlementReportRepository settlementReportRepository;

	@Test
	void findsStatementsFilteredByAssignor() {
		Currency brl = currencyRepository.findByCode("BRL").orElseThrow();
		ReceivableType duplicata = receivableTypeRepository.findByCode(ReceivableTypeCode.DUPLICATA_MERCANTIL).orElseThrow();
		Assignor assignorA = assignorRepository.save(new Assignor("Cedente Report A", "55666777000188"));
		Assignor assignorB = assignorRepository.save(new Assignor("Cedente Report B", "66777888000199"));
		LocalDate referenceDate = LocalDate.of(2026, 7, 7);

		Receivable receivableA = receivableRepository.save(new Receivable(
				assignorA, duplicata, brl, new BigDecimal("1000.00"), "DOC-RPT-A", referenceDate, referenceDate.plusDays(30)));
		Receivable receivableB = receivableRepository.save(new Receivable(
				assignorB, duplicata, brl, new BigDecimal("2000.00"), "DOC-RPT-B", referenceDate, referenceDate.plusDays(30)));

		BaseRate baseRate = new BaseRate(new BigDecimal("2.0"), referenceDate);
		settlementService.settle(receivableA.getId(), "BRL", baseRate);
		settlementService.settle(receivableB.getId(), "BRL", baseRate);

		List<SettlementStatementRow> onlyA =
				settlementReportRepository.findStatements(null, null, assignorA.getId(), 0, 50);

		assertThat(onlyA).extracting(SettlementStatementRow::documentNumber).containsExactly("DOC-RPT-A");
		assertThat(onlyA.get(0).fxRateUsed()).isNull();
	}
}
