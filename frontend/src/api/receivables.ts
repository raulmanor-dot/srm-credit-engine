import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { ReceivableRequest, ReceivableResponse, ReceivableStatus } from './types';

export function useReceivables(status?: ReceivableStatus) {
	return useQuery({
		queryKey: ['receivables', { status }],
		queryFn: () => {
			const query = status ? `?status=${status}` : '';
			return httpClient.get<ReceivableResponse[]>(`/receivables${query}`);
		},
	});
}

export function useCreateReceivable() {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (request: ReceivableRequest) => httpClient.post<ReceivableResponse>('/receivables', request),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['receivables'] });
		},
	});
}
