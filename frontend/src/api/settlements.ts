import { useMutation, useQueryClient } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { SettlementRequest, SettlementResponse } from './types';

export function useSettle() {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (request: SettlementRequest) => httpClient.post<SettlementResponse>('/settlements', request),
		onSuccess: () => {
			// A liquidação muda o status do recebível (some da lista PENDING) e
			// gera uma nova linha no extrato — ambas as queries ficam obsoletas.
			queryClient.invalidateQueries({ queryKey: ['receivables'] });
			queryClient.invalidateQueries({ queryKey: ['settlement-report'] });
		},
	});
}
