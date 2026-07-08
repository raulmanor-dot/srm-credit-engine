package com.srmasset.creditengine.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.pricing.BaseRate;
import com.srmasset.creditengine.domain.service.SettlementBatchService.BatchSettlementItemResult;
import com.srmasset.creditengine.persistence.entity.Settlement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettlementBatchServiceTest {

	@Mock
	private SettlementService settlementService;

	@Mock
	private Settlement settlementOk1;

	@Mock
	private Settlement settlementOk3;

	@Test
	void aFailingItemDoesNotStopOrRollBackTheOthers() {
		BaseRate baseRate = new BaseRate(new BigDecimal("2.0"), LocalDate.now());
		when(settlementOk1.getId()).thenReturn(101L);
		when(settlementOk3.getId()).thenReturn(103L);
		when(settlementService.settle(1L, "BRL", baseRate)).thenReturn(settlementOk1);
		when(settlementService.settle(2L, "BRL", baseRate)).thenThrow(new ReceivableNotFoundException(2L));
		when(settlementService.settle(3L, "BRL", baseRate)).thenReturn(settlementOk3);

		SettlementBatchService batchService = new SettlementBatchService(settlementService);
		List<BatchSettlementItemResult> results = batchService.settleBatch(List.of(1L, 2L, 3L), "BRL", baseRate);

		assertThat(results).hasSize(3);
		assertThat(results.get(0).success()).isTrue();
		assertThat(results.get(0).settlementId()).isEqualTo(101L);
		assertThat(results.get(1).success()).isFalse();
		assertThat(results.get(1).errorMessage()).contains("2");
		assertThat(results.get(2).success()).isTrue();
		assertThat(results.get(2).settlementId()).isEqualTo(103L);
	}
}
