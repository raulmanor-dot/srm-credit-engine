package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.persistence.report.SettlementReportRepository;
import com.srmasset.creditengine.persistence.report.SettlementStatementRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exceção arquitetural deliberada: este controller fala direto com
 * {@link SettlementReportRepository} (SQL nativo), pulando
 * {@code SettlementService}, a camada de domínio e as entidades JPA —
 * exatamente o caminho paralelo que o item 3.6 do enunciado permite para o
 * extrato de liquidação. É leitura pura para relatório, sem invariante de
 * negócio a proteger, então não há razão para passar pela camada de
 * negócio. Ver README (seção Arquitetura) e ADR correspondente.
 *
 * <p>Retorna {@link SettlementStatementRow} (tipo de {@code persistence.report})
 * diretamente na resposta HTTP, em vez de remapear para um DTO de
 * {@code application.dto} — isso é intencional: remapear escondería o
 * próprio ponto que este endpoint demonstra.
 */
@RestController
public class ReportController {

	private final SettlementReportRepository settlementReportRepository;

	public ReportController(SettlementReportRepository settlementReportRepository) {
		this.settlementReportRepository = settlementReportRepository;
	}

	@GetMapping("/reports/settlements")
	public List<SettlementStatementRow> settlementStatements(
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) Long assignorId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return settlementReportRepository.findStatements(fromDate, toDate, assignorId, page, size);
	}
}
