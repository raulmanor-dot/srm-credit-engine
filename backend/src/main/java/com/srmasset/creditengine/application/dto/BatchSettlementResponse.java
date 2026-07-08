package com.srmasset.creditengine.application.dto;

import java.util.List;

public record BatchSettlementResponse(List<BatchSettlementItemResponse> results) {}
