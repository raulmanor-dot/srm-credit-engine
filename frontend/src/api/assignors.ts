import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { AssignorRequest, AssignorResponse } from './types';

export function useAssignors() {
	return useQuery({
		queryKey: ['assignors'],
		queryFn: () => httpClient.get<AssignorResponse[]>('/assignors'),
	});
}

export function useCreateAssignor() {
	const queryClient = useQueryClient();
	return useMutation({
		mutationFn: (request: AssignorRequest) => httpClient.post<AssignorResponse>('/assignors', request),
		onSuccess: () => {
			queryClient.invalidateQueries({ queryKey: ['assignors'] });
		},
	});
}
