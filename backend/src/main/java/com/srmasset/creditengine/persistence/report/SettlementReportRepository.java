package com.srmasset.creditengine.persistence.report;

import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Caminho paralelo deliberado (item 3.6 do enunciado, ver README): o {@code ReportController} fala
 * diretamente com este repositório, com SQL nativo, sem passar por {@code SettlementService},
 * {@code PricingStrategy} ou qualquer entidade JPA. É leitura pura para relatório — não há regra de
 * negócio a proteger aqui, então o atalho arquitetural é aceitável.
 *
 * <p>Sem query de {@code COUNT(*)} companion (sem total de páginas) — mantém este caminho
 * deliberadamente mínimo; adicione uma se uma UI precisar de indicador de páginas.
 */
@Repository
public class SettlementReportRepository {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private static final String STATEMENT_SQL =
            """
			SELECT s.id AS settlement_id,
			       r.document_number,
			       a.name AS assignor_name,
			       s.face_value,
			       fc.code AS face_value_currency_code,
			       s.present_value_face_currency,
			       s.net_value_payment_currency,
			       pc.code AS payment_currency_code,
			       s.base_rate_used,
			       s.spread_used,
			       s.fx_rate_used,
			       s.settled_at
			FROM settlements s
			JOIN receivables r ON r.id = s.receivable_id
			JOIN assignors a ON a.id = r.assignor_id
			JOIN currencies fc ON fc.id = s.face_value_currency_id
			JOIN currencies pc ON pc.id = s.payment_currency_id
			WHERE (:fromDate::date IS NULL OR s.settled_at >= :fromDate::date)
			  AND (:toDate::date IS NULL OR s.settled_at < (:toDate::date + INTERVAL '1 day')::timestamptz)
			  AND (:assignorId::bigint IS NULL OR r.assignor_id = :assignorId::bigint)
			ORDER BY s.settled_at DESC
			LIMIT :limit OFFSET :offset
			""";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SettlementReportRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SettlementStatementRow> findStatements(
            LocalDate fromDate, LocalDate toDate, Long assignorId, int page, int size) {
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int offset = Math.max(page, 0) * pageSize;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("fromDate", fromDate, Types.DATE)
                        .addValue("toDate", toDate, Types.DATE)
                        .addValue("assignorId", assignorId, Types.BIGINT)
                        .addValue("limit", pageSize)
                        .addValue("offset", offset);

        return jdbcTemplate.query(
                STATEMENT_SQL,
                params,
                (rs, rowNum) ->
                        new SettlementStatementRow(
                                rs.getLong("settlement_id"),
                                rs.getString("document_number"),
                                rs.getString("assignor_name"),
                                rs.getBigDecimal("face_value"),
                                rs.getString("face_value_currency_code"),
                                rs.getBigDecimal("present_value_face_currency"),
                                rs.getBigDecimal("net_value_payment_currency"),
                                rs.getString("payment_currency_code"),
                                rs.getBigDecimal("base_rate_used"),
                                rs.getBigDecimal("spread_used"),
                                rs.getBigDecimal("fx_rate_used"),
                                rs.getObject("settled_at", OffsetDateTime.class)));
    }
}
