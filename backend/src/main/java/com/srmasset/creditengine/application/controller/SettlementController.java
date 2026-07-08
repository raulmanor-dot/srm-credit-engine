package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.BatchSettlementItemResponse;
import com.srmasset.creditengine.application.dto.BatchSettlementRequest;
import com.srmasset.creditengine.application.dto.BatchSettlementResponse;
import com.srmasset.creditengine.application.dto.SettlementRequest;
import com.srmasset.creditengine.application.dto.SettlementResponse;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.service.SettlementBatchService;
import com.srmasset.creditengine.domain.service.SettlementBatchService.BatchSettlementItemResult;
import com.srmasset.creditengine.domain.service.SettlementService;
import com.srmasset.creditengine.persistence.entity.Settlement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementBatchService settlementBatchService;

    public SettlementController(
            SettlementService settlementService, SettlementBatchService settlementBatchService) {
        this.settlementService = settlementService;
        this.settlementBatchService = settlementBatchService;
    }

    @PostMapping("/settlements")
    public SettlementResponse settle(@Valid @RequestBody SettlementRequest request) {
        LocalDate referenceDate =
                request.referenceDate() != null ? request.referenceDate() : LocalDate.now();
        BaseRate baseRate = new BaseRate(request.baseRateMonthlyPercent(), referenceDate);

        Settlement settlement =
                settlementService.settle(
                        request.receivableId(), request.paymentCurrencyCode(), baseRate);

        return toResponse(settlement);
    }

    @PostMapping("/settlements/batch")
    public BatchSettlementResponse settleBatch(@Valid @RequestBody BatchSettlementRequest request) {
        LocalDate referenceDate =
                request.referenceDate() != null ? request.referenceDate() : LocalDate.now();
        BaseRate baseRate = new BaseRate(request.baseRateMonthlyPercent(), referenceDate);

        List<BatchSettlementItemResult> results =
                settlementBatchService.settleBatch(
                        request.receivableIds(), request.paymentCurrencyCode(), baseRate);

        List<BatchSettlementItemResponse> responses =
                results.stream()
                        .map(
                                r ->
                                        new BatchSettlementItemResponse(
                                                r.receivableId(),
                                                r.success(),
                                                r.settlementId(),
                                                r.errorMessage()))
                        .toList();

        return new BatchSettlementResponse(responses);
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getReceivable().getId(),
                settlement.getFaceValue(),
                settlement.getFaceValueCurrency().getCode(),
                settlement.getPresentValueFaceCurrency(),
                settlement.getNetValuePaymentCurrency(),
                settlement.getPaymentCurrency().getCode(),
                settlement.getFxRateUsed(),
                settlement.getBaseRateUsed(),
                settlement.getSpreadUsed(),
                settlement.getTermDays(),
                settlement.getTermMonths(),
                settlement.getSettledAt());
    }
}
