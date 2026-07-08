import { useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { AssignorResponse } from './types';

export function useAssignors() {
	return useQuery({
		queryKey: ['assignors'],
		queryFn: () => httpClient.get<AssignorResponse[]>('/assignors'),
	});
}
