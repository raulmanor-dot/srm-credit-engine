import { useState } from 'react';
import { ActionIcon, Container, Group, Select, Table, Text, Title } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { IconChevronLeft, IconChevronRight } from '@tabler/icons-react';
import { useAssignors } from '../api/assignors';
import { useCurrencies } from '../api/currencies';
import { SETTLEMENT_REPORT_PAGE_SIZE, useSettlementReport } from '../api/settlementReport';

// "Grid de Transações": histórico de liquidações com paginação server-side
// (GET /reports/settlements?page=&size=) e filtros dinâmicos (data, cedente, moeda).
// O backend não expõe total de páginas (decisão deliberada, ver README) —
// "próxima página" fica habilitada enquanto a página atual vier cheia.
export function TransactionsGridPage() {
	const [page, setPage] = useState(0);
	const [fromDate, setFromDate] = useState<string | null>(null);
	const [toDate, setToDate] = useState<string | null>(null);
	const [assignorId, setAssignorId] = useState<string | null>(null);
	const [paymentCurrencyCode, setPaymentCurrencyCode] = useState<string | null>(null);

	const { data: assignors } = useAssignors();
	const { data: currencies } = useCurrencies();
	const { data: rows, isFetching } = useSettlementReport(page, {
		fromDate: fromDate ?? undefined,
		toDate: toDate ?? undefined,
		assignorId: assignorId ? Number(assignorId) : undefined,
		paymentCurrencyCode: paymentCurrencyCode ?? undefined,
	});

	const assignorOptions = (assignors ?? []).map((a) => ({ value: String(a.id), label: a.name }));
	const currencyOptions = (currencies ?? []).map((c) => ({ value: c.code, label: c.code }));
	const hasNextPage = (rows?.length ?? 0) === SETTLEMENT_REPORT_PAGE_SIZE;

	function resetToFirstPage<T>(setter: (value: T) => void) {
		return (value: T) => {
			setPage(0);
			setter(value);
		};
	}

	return (
		<Container size="xl">
			<Title order={2} mb="lg">
				Grid de Transações
			</Title>

			<Group mb="md">
				<DateInput label="De" value={fromDate} onChange={resetToFirstPage(setFromDate)} clearable />
				<DateInput label="Até" value={toDate} onChange={resetToFirstPage(setToDate)} clearable />
				<Select
					label="Cedente"
					placeholder="Todos"
					data={assignorOptions}
					value={assignorId}
					onChange={resetToFirstPage(setAssignorId)}
					clearable
					searchable
				/>
				<Select
					label="Moeda de pagamento"
					placeholder="Todas"
					data={currencyOptions}
					value={paymentCurrencyCode}
					onChange={resetToFirstPage(setPaymentCurrencyCode)}
					clearable
				/>
			</Group>

			<Table striped highlightOnHover>
				<Table.Thead>
					<Table.Tr>
						<Table.Th>ID</Table.Th>
						<Table.Th>Documento</Table.Th>
						<Table.Th>Cedente</Table.Th>
						<Table.Th>Valor de face</Table.Th>
						<Table.Th>Valor presente</Table.Th>
						<Table.Th>Valor líquido pago</Table.Th>
						<Table.Th>Câmbio</Table.Th>
						<Table.Th>Liquidado em</Table.Th>
					</Table.Tr>
				</Table.Thead>
				<Table.Tbody>
					{(rows ?? []).map((row) => (
						<Table.Tr key={row.settlementId}>
							<Table.Td>{row.settlementId}</Table.Td>
							<Table.Td>{row.documentNumber ?? '—'}</Table.Td>
							<Table.Td>{row.assignorName}</Table.Td>
							<Table.Td>
								{row.faceValue.toLocaleString('pt-BR', { style: 'currency', currency: row.faceValueCurrencyCode })}
							</Table.Td>
							<Table.Td>
								{row.presentValueFaceCurrency.toLocaleString('pt-BR', {
									style: 'currency',
									currency: row.faceValueCurrencyCode,
								})}
							</Table.Td>
							<Table.Td>
								{row.netValuePaymentCurrency.toLocaleString('pt-BR', {
									style: 'currency',
									currency: row.paymentCurrencyCode,
								})}
							</Table.Td>
							<Table.Td>{row.fxRateUsed ?? '—'}</Table.Td>
							<Table.Td>{new Date(row.settledAt).toLocaleString('pt-BR')}</Table.Td>
						</Table.Tr>
					))}
				</Table.Tbody>
			</Table>

			{!isFetching && (rows?.length ?? 0) === 0 && (
				<Text c="dimmed" ta="center" mt="md">
					Nenhuma liquidação encontrada para os filtros selecionados.
				</Text>
			)}

			<Group justify="center" mt="md" gap="xs">
				<ActionIcon variant="default" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
					<IconChevronLeft size={16} />
				</ActionIcon>
				<Text size="sm">Página {page + 1}</Text>
				<ActionIcon variant="default" disabled={!hasNextPage} onClick={() => setPage((p) => p + 1)}>
					<IconChevronRight size={16} />
				</ActionIcon>
			</Group>
		</Container>
	);
}
