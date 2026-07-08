import { useState } from 'react';
import { Alert, Button, Group, Modal, NumberInput, Select, Stack, TextInput } from '@mantine/core';
import { DateInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useAssignors, useCreateAssignor } from '../api/assignors';
import { useCurrencies } from '../api/currencies';
import { useCreateReceivable } from '../api/receivables';
import { useReceivableTypes } from '../api/receivableTypes';
import { ApiError } from '../api/httpClient';
import type { ReceivableResponse } from '../api/types';

interface FormValues {
	assignorId: string | null;
	receivableTypeId: string | null;
	faceValueCurrencyId: string | null;
	faceValue: number | '';
	documentNumber: string;
	issueDate: string | null;
	dueDate: string | null;
}

interface NewReceivableModalProps {
	opened: boolean;
	onClose: () => void;
	onCreated: (receivable: ReceivableResponse) => void;
}

export function NewReceivableModal({ opened, onClose, onCreated }: NewReceivableModalProps) {
	const { data: assignors } = useAssignors();
	const { data: receivableTypes } = useReceivableTypes();
	const { data: currencies } = useCurrencies();
	const createReceivable = useCreateReceivable();
	const createAssignor = useCreateAssignor();

	const [showNewAssignor, setShowNewAssignor] = useState(false);
	const [newAssignorName, setNewAssignorName] = useState('');
	const [newAssignorTaxId, setNewAssignorTaxId] = useState('');

	const form = useForm<FormValues>({
		initialValues: {
			assignorId: null,
			receivableTypeId: null,
			faceValueCurrencyId: null,
			faceValue: '',
			documentNumber: '',
			issueDate: new Date().toISOString().slice(0, 10),
			dueDate: null,
		},
		validate: {
			assignorId: (value) => (value ? null : 'Obrigatório'),
			receivableTypeId: (value) => (value ? null : 'Obrigatório'),
			faceValueCurrencyId: (value) => (value ? null : 'Obrigatório'),
			faceValue: (value) => (value && value > 0 ? null : 'Deve ser positivo'),
			issueDate: (value) => (value ? null : 'Obrigatório'),
			dueDate: (value, values) =>
				value && values.issueDate && value > values.issueDate ? null : 'Deve ser após a emissão',
		},
	});

	async function handleCreateAssignor() {
		if (!newAssignorName.trim() || !newAssignorTaxId.trim()) return;
		try {
			const assignor = await createAssignor.mutateAsync({ name: newAssignorName, taxId: newAssignorTaxId });
			form.setFieldValue('assignorId', String(assignor.id));
			setShowNewAssignor(false);
			setNewAssignorName('');
			setNewAssignorTaxId('');
			notifications.show({ message: `Cedente "${assignor.name}" criado`, color: 'green' });
		} catch (error) {
			notifications.show({
				title: 'Falha ao criar cedente',
				message: error instanceof ApiError ? error.message : 'Erro inesperado',
				color: 'red',
			});
		}
	}

	async function handleSubmit(values: FormValues) {
		try {
			const receivable = await createReceivable.mutateAsync({
				assignorId: Number(values.assignorId),
				receivableTypeId: Number(values.receivableTypeId),
				faceValueCurrencyId: Number(values.faceValueCurrencyId),
				faceValue: values.faceValue as number,
				documentNumber: values.documentNumber || undefined,
				issueDate: values.issueDate!,
				dueDate: values.dueDate!,
			});
			notifications.show({ message: `Recebível #${receivable.id} criado`, color: 'green' });
			form.reset();
			onCreated(receivable);
			onClose();
		} catch (error) {
			notifications.show({
				title: 'Falha ao criar recebível',
				message: error instanceof ApiError ? error.message : 'Erro inesperado',
				color: 'red',
			});
		}
	}

	const assignorOptions = (assignors ?? []).map((a) => ({ value: String(a.id), label: a.name }));
	const typeOptions = (receivableTypes ?? []).map((t) => ({ value: String(t.id), label: t.name }));
	const currencyOptions = (currencies ?? []).map((c) => ({ value: String(c.id), label: `${c.code} — ${c.name}` }));

	return (
		<Modal opened={opened} onClose={onClose} title="Novo recebível" size="md">
			<form onSubmit={form.onSubmit(handleSubmit)}>
				<Stack>
					<Select
						label="Cedente"
						placeholder="Selecione um cedente"
						data={assignorOptions}
						searchable
						{...form.getInputProps('assignorId')}
					/>
					<Button variant="subtle" size="xs" onClick={() => setShowNewAssignor((v) => !v)}>
						{showNewAssignor ? 'Cancelar' : '+ Novo cedente'}
					</Button>

					{showNewAssignor && (
						<Alert variant="light" color="gray">
							<Stack gap="xs">
								<TextInput
									label="Nome do cedente"
									value={newAssignorName}
									onChange={(e) => setNewAssignorName(e.currentTarget.value)}
								/>
								<TextInput
									label="CPF/CNPJ"
									value={newAssignorTaxId}
									onChange={(e) => setNewAssignorTaxId(e.currentTarget.value)}
								/>
								<Button size="xs" loading={createAssignor.isPending} onClick={handleCreateAssignor}>
									Criar cedente
								</Button>
							</Stack>
						</Alert>
					)}

					<Select
						label="Tipo"
						placeholder="Selecione o tipo"
						data={typeOptions}
						{...form.getInputProps('receivableTypeId')}
					/>

					<Select
						label="Moeda"
						placeholder="Selecione a moeda"
						data={currencyOptions}
						{...form.getInputProps('faceValueCurrencyId')}
					/>

					<NumberInput label="Valor de face" min={0.01} decimalScale={2} {...form.getInputProps('faceValue')} />

					<TextInput label="Número do documento" {...form.getInputProps('documentNumber')} />

					<DateInput label="Data de emissão" {...form.getInputProps('issueDate')} />
					<DateInput label="Data de vencimento" {...form.getInputProps('dueDate')} />

					<Group justify="flex-end">
						<Button variant="default" onClick={onClose}>
							Cancelar
						</Button>
						<Button type="submit" loading={createReceivable.isPending}>
							Criar recebível
						</Button>
					</Group>
				</Stack>
			</form>
		</Modal>
	);
}
