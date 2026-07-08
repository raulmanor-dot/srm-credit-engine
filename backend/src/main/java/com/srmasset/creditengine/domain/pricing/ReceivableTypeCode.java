package com.srmasset.creditengine.domain.pricing;

/**
 * Discriminador usado pelo {@link PricingStrategyResolver} para mapear cada tipo de recebível ao
 * seu bean de {@link PricingStrategy}. O percentual de spread em si não vive aqui: fica na tabela
 * receivable_types (configurável).
 */
public enum ReceivableTypeCode {
    DUPLICATA_MERCANTIL,
    CHEQUE_PRE_DATADO
}
