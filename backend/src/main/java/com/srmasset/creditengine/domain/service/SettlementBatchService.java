package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.persistence.entity.Settlement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Orquestra a liquidação de um lote de recebíveis, item a item (ver ADR 0004): a falha de um item
 * não reverte nem bloqueia os demais.
 *
 * <p>Isso só funciona porque {@link SettlementService#settle} está em um bean <b>separado</b>
 * injetado aqui — chamar {@code this.settle(...)} de dentro da própria classe não passaria pelo
 * proxy do Spring e o {@code @Transactional(REQUIRES_NEW)} nunca seria acionado (auto-invocação é a
 * armadilha clássica de AOP baseado em proxy). Injetando {@link SettlementService} como
 * colaborador, cada chamada é uma chamada externa de verdade ao proxy, então cada item abre e fecha
 * sua própria transação independente, com sucesso ou falha isolados dos outros itens.
 *
 * <p>Deliberadamente sem {@code @Transactional} aqui: envolver o laço numa transação não mudaria o
 * isolamento por item ({@code REQUIRES_NEW} sempre suspende qualquer transação em curso), só
 * manteria uma conexão aberta pela duração inteira do lote sem necessidade.
 */
@Service
public class SettlementBatchService {

    private final SettlementService settlementService;

    public SettlementBatchService(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    public List<BatchSettlementItemResult> settleBatch(
            List<Long> receivableIds, String paymentCurrencyCode, BaseRate baseRate) {
        List<BatchSettlementItemResult> results = new ArrayList<>();
        for (Long receivableId : receivableIds) {
            try {
                Settlement settlement =
                        settlementService.settle(receivableId, paymentCurrencyCode, baseRate);
                results.add(BatchSettlementItemResult.success(receivableId, settlement.getId()));
            } catch (ObjectOptimisticLockingFailureException ex) {
                results.add(
                        BatchSettlementItemResult.failure(
                                receivableId,
                                "Concurrent update detected for receivable "
                                        + receivableId
                                        + ", please retry"));
            } catch (RuntimeException ex) {
                results.add(BatchSettlementItemResult.failure(receivableId, ex.getMessage()));
            }
        }
        return results;
    }

    public record BatchSettlementItemResult(
            Long receivableId, boolean success, Long settlementId, String errorMessage) {

        static BatchSettlementItemResult success(Long receivableId, Long settlementId) {
            return new BatchSettlementItemResult(receivableId, true, settlementId, null);
        }

        static BatchSettlementItemResult failure(Long receivableId, String errorMessage) {
            return new BatchSettlementItemResult(receivableId, false, null, errorMessage);
        }
    }
}
