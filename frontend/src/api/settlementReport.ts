import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { SettlementStatementRow } from './types';

export interface SettlementReportFilters {
	fromDate?: string;
	toDate?: string;
	assignorId?: number;
}

export const SETTLEMENT_REPORT_PAGE_SIZE = 20;

// GET /reports/settlements não devolve total de páginas (decisão deliberada
// do backend — ver SettlementReportRepository). Paginação "tem próxima
// página" é inferida pelo tamanho da página retornada: se veio cheia, pode
// haver mais; se veio incompleta, é a última.
export function useSettlementReport(page: number, filters: SettlementReportFilters) {
	return useQuery({
		queryKey: ['settlement-report', page, filters],
		queryFn: () => {
			const params = new URLSearchParams({ page: String(page), size: String(SETTLEMENT_REPORT_PAGE_SIZE) });
			if (filters.fromDate) params.set('fromDate', filters.fromDate);
			if (filters.toDate) params.set('toDate', filters.toDate);
			if (filters.assignorId) params.set('assignorId', String(filters.assignorId));
			return httpClient.get<SettlementStatementRow[]>(`/reports/settlements?${params.toString()}`);
		},
		placeholderData: keepPreviousData,
	});
}
