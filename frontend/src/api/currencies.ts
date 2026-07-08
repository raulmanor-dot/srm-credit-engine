import { useQuery } from '@tanstack/react-query';
import { httpClient } from './httpClient';
import type { CurrencyResponse } from './types';

export function useCurrencies() {
	return useQuery({
		queryKey: ['currencies'],
		queryFn: () => httpClient.get<CurrencyResponse[]>('/currencies'),
	});
}
