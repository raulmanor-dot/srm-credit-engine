package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.SimulationRequest;
import com.srmasset.creditengine.application.dto.SimulationResponse;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.service.SimulationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fonte única de verdade da fórmula de precificação: o frontend NÃO replica este cálculo — o painel
 * de simulação chama este endpoint com debounce.
 */
@RestController
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/simulations")
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        LocalDate referenceDate =
                request.referenceDate() != null ? request.referenceDate() : LocalDate.now();
        BaseRate baseRate = new BaseRate(request.baseRateMonthlyPercent(), referenceDate);

        var result = simulationService.simulate(request.receivableId(), baseRate);

        return new SimulationResponse(
                result.receivableId(),
                result.faceValue(),
                result.presentValue(),
                result.termInMonths(),
                result.currencyCode());
    }
}
