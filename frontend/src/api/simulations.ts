import { useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { SimulationRequest, SimulationResponse } from './types';

// POST /simulations é usado como RPC de leitura (sem efeito colateral) —
// por isso useQuery (com refetch automático a cada mudança debounced de
// parâmetro) em vez de useMutation.
export function useSimulation(request: SimulationRequest | null) {
	return useQuery({
		queryKey: ['simulation', request],
		queryFn: () => httpClient.post<SimulationResponse>('/simulations', request),
		enabled: request !== null,
		retry: false,
	});
}
