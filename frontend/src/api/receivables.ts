import { useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { ReceivableResponse, ReceivableStatus } from './types';

export function useReceivables(status?: ReceivableStatus) {
	return useQuery({
		queryKey: ['receivables', { status }],
		queryFn: () => {
			const query = status ? `?status=${status}` : '';
			return httpClient.get<ReceivableResponse[]>(`/receivables${query}`);
		},
	});
}
