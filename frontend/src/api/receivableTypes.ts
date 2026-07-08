import { useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { ReceivableTypeResponse } from './types';

export function useReceivableTypes() {
	return useQuery({
		queryKey: ['receivable-types'],
		queryFn: () => httpClient.get<ReceivableTypeResponse[]>('/receivable-types'),
	});
}
