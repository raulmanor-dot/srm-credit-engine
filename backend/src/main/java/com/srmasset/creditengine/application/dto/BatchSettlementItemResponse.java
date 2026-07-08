package com.srmasset.creditengine.application.dto;

public record BatchSettlementItemResponse(Long receivableId, boolean success, Long settlementId, String errorMessage) {
}
