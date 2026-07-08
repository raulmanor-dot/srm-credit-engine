package com.srmasset.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srmasset.creditengine.domain.exception.UnsupportedReceivableTypeException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingStrategyResolverTest {

    private final PricingStrategyResolver resolver =
            new PricingStrategyResolver(
                    List.of(new DuplicataMercantilStrategy(), new ChequePreDatadoStrategy()));

    @Test
    void resolvesEachRegisteredType() {
        assertThat(resolver.resolve(ReceivableTypeCode.DUPLICATA_MERCANTIL))
                .isInstanceOf(DuplicataMercantilStrategy.class);
        assertThat(resolver.resolve(ReceivableTypeCode.CHEQUE_PRE_DATADO))
                .isInstanceOf(ChequePreDatadoStrategy.class);
    }

    @Test
    void throwsForUnregisteredType() {
        PricingStrategyResolver emptyResolver = new PricingStrategyResolver(List.of());

        assertThatThrownBy(() -> emptyResolver.resolve(ReceivableTypeCode.DUPLICATA_MERCANTIL))
                .isInstanceOf(UnsupportedReceivableTypeException.class);
    }
}
