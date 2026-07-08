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

export interface AssignorRequest {
	name: string;
	taxId: string;
}

export interface CurrencyResponse {
	id: number;
	code: string;
	name: string;
	active: boolean;
	createdAt: string;
}

export interface ReceivableTypeResponse {
	id: number;
	code: string;
	name: string;
	spreadPercentMonthly: number;
}

export interface ReceivableRequest {
	assignorId: number;
	receivableTypeId: number;
	faceValueCurrencyId: number;
	faceValue: number;
	documentNumber?: string;
	issueDate: string;
	dueDate: string;
}

export interface SettlementRequest {
	receivableId: number;
	paymentCurrencyCode: string;
	baseRateMonthlyPercent: number;
	referenceDate?: string;
}

export interface SettlementResponse {
	settlementId: number;
	receivableId: number;
	faceValue: number;
	faceValueCurrencyCode: string;
	presentValueFaceCurrency: number;
	netValuePaymentCurrency: number;
	paymentCurrencyCode: string;
	fxRateUsed: number | null;
	baseRateUsed: number;
	spreadUsed: number;
	termDays: number;
	termMonths: number;
	settledAt: string;
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
