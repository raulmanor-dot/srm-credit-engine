import { useMemo, useState } from 'react';
import { Card, Container, Grid, NumberInput, Select, Skeleton, Stack, Text, Title } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { useDebouncedValue } from '@mantine/hooks';
import { useReceivables } from '../api/receivables';
import { useSimulation } from '../api/simulations';
import type { SimulationRequest } from '../api/types';

// "Painel do Operador": mostra os dados do recebível (Valor, Vencimento,
// Tipo) e recalcula o valor líquido em tempo real (debounce) conforme o
// operador ajusta a taxa base — o cálculo em si vive só no backend
// (PricingStrategy), este painel só é uma vitrine reativa para POST
// /simulations.
export function OperatorPanelPage() {
	const { data: receivables, isLoading: isLoadingReceivables } = useReceivables('PENDING');
	const [receivableId, setReceivableId] = useState<string | null>(null);
	const [baseRate, setBaseRate] = useState<number | ''>(2.0);
	const [referenceDate, setReferenceDate] = useState<string | null>(new Date().toISOString().slice(0, 10));

	const [debouncedBaseRate] = useDebouncedValue(baseRate, 400);
	const [debouncedReferenceDate] = useDebouncedValue(referenceDate, 400);

	const selectedReceivable = receivables?.find((r) => String(r.id) === receivableId) ?? null;

	const simulationRequest = useMemo<SimulationRequest | null>(() => {
		if (!selectedReceivable || debouncedBaseRate === '' || !debouncedReferenceDate) {
			return null;
		}
		return {
			receivableId: selectedReceivable.id,
			baseRateMonthlyPercent: debouncedBaseRate,
			referenceDate: debouncedReferenceDate,
		};
	}, [selectedReceivable, debouncedBaseRate, debouncedReferenceDate]);

	const { data: simulation, isFetching: isSimulating, error } = useSimulation(simulationRequest);

	const receivableOptions = (receivables ?? []).map((r) => ({
		value: String(r.id),
		label: `#${r.id} — ${r.documentNumber ?? 'sem documento'} — ${r.assignorName} (${r.faceValue.toLocaleString('pt-BR', { style: 'currency', currency: r.faceValueCurrencyCode })})`,
	}));

	return (
		<Container size="md">
			<Title order={2} mb="lg">
				Painel do Operador
			</Title>

			<Grid>
				<Grid.Col span={{ base: 12, md: 6 }}>
					<Card withBorder padding="lg">
						<Stack>
							<Select
								label="Recebível"
								placeholder={isLoadingReceivables ? 'Carregando...' : 'Selecione um recebível pendente'}
								data={receivableOptions}
								value={receivableId}
								onChange={setReceivableId}
								searchable
								disabled={isLoadingReceivables}
								nothingFoundMessage="Nenhum recebível pendente"
							/>

							{selectedReceivable && (
								<Stack gap={4}>
									<Text size="sm" c="dimmed">
										Tipo: {selectedReceivable.receivableTypeCode}
									</Text>
									<Text size="sm" c="dimmed">
										Valor de face:{' '}
										{selectedReceivable.faceValue.toLocaleString('pt-BR', {
											style: 'currency',
											currency: selectedReceivable.faceValueCurrencyCode,
										})}
									</Text>
									<Text size="sm" c="dimmed">
										Vencimento: {new Date(selectedReceivable.dueDate).toLocaleDateString('pt-BR')}
									</Text>
								</Stack>
							)}

							<NumberInput
								label="Taxa base (% a.m.)"
								value={baseRate}
								onChange={(value) => setBaseRate(typeof value === 'number' ? value : '')}
								min={0}
								step={0.1}
								decimalScale={4}
								disabled={!selectedReceivable}
							/>

							<DateInput label="Data de referência" value={referenceDate} onChange={setReferenceDate} disabled={!selectedReceivable} />
						</Stack>
					</Card>
				</Grid.Col>

				<Grid.Col span={{ base: 12, md: 6 }}>
					<Card withBorder padding="lg" h="100%">
						<Stack>
							<Title order={4}>Valor líquido (simulação)</Title>

							{!selectedReceivable && (
								<Text c="dimmed">Selecione um recebível para simular.</Text>
							)}

							{selectedReceivable && isSimulating && <Skeleton height={80} />}

							{selectedReceivable && error && (
								<Text c="red">{error instanceof Error ? error.message : 'Falha ao simular'}</Text>
							)}

							{selectedReceivable && simulation && !isSimulating && (
								<Stack gap={4}>
									<Text size="2rem" fw={700}>
										{simulation.presentValue.toLocaleString('pt-BR', { style: 'currency', currency: simulation.currencyCode })}
									</Text>
									<Text size="sm" c="dimmed">
										Prazo: {simulation.termInMonths.toFixed(2)} meses
									</Text>
									<Text size="sm" c="dimmed">
										Valor de face: {simulation.faceValue.toLocaleString('pt-BR', { style: 'currency', currency: simulation.currencyCode })}
									</Text>
								</Stack>
							)}
						</Stack>
					</Card>
				</Grid.Col>
			</Grid>
		</Container>
	);
}
