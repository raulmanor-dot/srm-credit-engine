import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Container, Divider, Grid, Group, NumberInput, Select, Skeleton, Stack, Text, Title } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { useDebouncedValue, useDisclosure } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { useCurrencies } from '../api/currencies';
import { ApiError } from '../api/httpClient';
import { useReceivables } from '../api/receivables';
import { useSettle } from '../api/settlements';
import { useSimulation } from '../api/simulations';
import type { ReceivableResponse, SimulationRequest } from '../api/types';
import { NewReceivableModal } from '../components/NewReceivableModal';

// "Painel do Operador": mostra os dados do recebível (Valor, Vencimento,
// Tipo) e recalcula o valor líquido em tempo real (debounce) conforme o
// operador ajusta a taxa base — o cálculo em si vive só no backend
// (PricingStrategy), este painel só é uma vitrine reativa para POST
// /simulations. "Liquidar" reusa os mesmos parâmetros da simulação: é
// literalmente o mesmo cálculo, só que persistido (POST /settlements).
export function OperatorPanelPage() {
	const {
		data: receivables,
		isLoading: isLoadingReceivables,
		error: receivablesError,
	} = useReceivables('PENDING');
	const { data: currencies } = useCurrencies();
	const [receivableId, setReceivableId] = useState<string | null>(null);
	const [baseRate, setBaseRate] = useState<number | ''>(2.0);
	const [referenceDate, setReferenceDate] = useState<string | null>(new Date().toISOString().slice(0, 10));
	const [paymentCurrencyId, setPaymentCurrencyId] = useState<string | null>(null);
	const [modalOpened, { open: openModal, close: closeModal }] = useDisclosure(false);

	const [debouncedBaseRate] = useDebouncedValue(baseRate, 400);
	const [debouncedReferenceDate] = useDebouncedValue(referenceDate, 400);

	const selectedReceivable = receivables?.find((r) => String(r.id) === receivableId) ?? null;

	// Ao trocar de recebível, a moeda de pagamento sugerida por padrão é a
	// própria moeda de face — o operador pode trocar se quiser liquidar em
	// outra moeda (exercita a conversão cambial do backend).
	useEffect(() => {
		if (selectedReceivable && currencies) {
			const faceCurrency = currencies.find((c) => c.code === selectedReceivable.faceValueCurrencyCode);
			setPaymentCurrencyId(faceCurrency ? String(faceCurrency.id) : null);
		}
	}, [selectedReceivable, currencies]);

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
	const settle = useSettle();

	const receivableOptions = (receivables ?? []).map((r) => ({
		value: String(r.id),
		label: `#${r.id} — ${r.documentNumber ?? 'sem documento'} — ${r.assignorName} (${r.faceValue.toLocaleString('pt-BR', { style: 'currency', currency: r.faceValueCurrencyCode })})`,
	}));
	const currencyOptions = (currencies ?? []).map((c) => ({ value: String(c.id), label: c.code }));

	function handleReceivableCreated(receivable: ReceivableResponse) {
		setReceivableId(String(receivable.id));
	}

	async function handleSettle() {
		if (!selectedReceivable || baseRate === '' || !referenceDate || !paymentCurrencyId) return;
		const paymentCurrencyCode = currencies?.find((c) => String(c.id) === paymentCurrencyId)?.code;
		if (!paymentCurrencyCode) return;

		try {
			const settlement = await settle.mutateAsync({
				receivableId: selectedReceivable.id,
				paymentCurrencyCode,
				baseRateMonthlyPercent: baseRate,
				referenceDate,
			});
			notifications.show({
				title: 'Recebível liquidado',
				message: `Valor líquido: ${settlement.netValuePaymentCurrency.toLocaleString('pt-BR', { style: 'currency', currency: settlement.paymentCurrencyCode })}`,
				color: 'green',
			});
			setReceivableId(null);
		} catch (settleError) {
			notifications.show({
				title: 'Falha ao liquidar',
				message: settleError instanceof ApiError ? settleError.message : 'Erro inesperado',
				color: 'red',
			});
		}
	}

	return (
		<Container size="md">
			<Group justify="space-between" mb="lg">
				<Title order={2}>Painel do Operador</Title>
				<Button variant="light" onClick={openModal}>
					+ Novo recebível
				</Button>
			</Group>

			<NewReceivableModal opened={modalOpened} onClose={closeModal} onCreated={handleReceivableCreated} />

			{receivablesError && (
				<Alert color="red" mb="lg" title="Não foi possível carregar os recebíveis pendentes">
					{receivablesError instanceof ApiError ? receivablesError.message : 'Verifique se a API está no ar.'}
				</Alert>
			)}

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

							{selectedReceivable && simulation && !isSimulating && (
								<>
									<Divider my="xs" />
									<Select
										label="Moeda de pagamento"
										data={currencyOptions}
										value={paymentCurrencyId}
										onChange={setPaymentCurrencyId}
									/>
									<Button onClick={handleSettle} loading={settle.isPending} disabled={!paymentCurrencyId}>
										Liquidar
									</Button>
								</>
							)}
						</Stack>
					</Card>
				</Grid.Col>
			</Grid>
		</Container>
	);
}
