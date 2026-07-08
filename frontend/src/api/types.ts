export type ReceivableStatus = 'PENDING' | 'SETTLED' | 'CANCELED';

export interface ReceivableResponse {
	id: number;
	assignorId: number;
	assignorName: string;
	receivableTypeId: number;
	receivableTypeCode: string;
	faceValueCurrencyCode: string;
	faceValue: number;
	documentNumber: string | null;
	issueDate: string;
	dueDate: string;
	status: ReceivableStatus;
	version: number;
	createdAt: string;
	updatedAt: string;
}

export interface AssignorResponse {
	id: number;
	name: string;
	taxId: string;
	active: boolean;
}

export interface SimulationRequest {
	receivableId: number;
	baseRateMonthlyPercent: number;
	referenceDate?: string;
}

export interface SimulationResponse {
	receivableId: number;
	faceValue: number;
	presentValue: number;
	termInMonths: number;
	currencyCode: string;
}

export interface SettlementStatementRow {
	settlementId: number;
	documentNumber: string | null;
	assignorName: string;
	faceValue: number;
	faceValueCurrencyCode: string;
	presentValueFaceCurrency: number;
	netValuePaymentCurrency: number;
	paymentCurrencyCode: string;
	baseRateUsed: number;
	spreadUsed: number;
	fxRateUsed: number | null;
	settledAt: string;
}
