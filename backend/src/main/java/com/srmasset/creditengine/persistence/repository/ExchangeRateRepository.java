package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.ExchangeRate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    // exchange_rates é append-only (ver V4/trigger): a taxa vigente é sempre a
    // linha mais recente por valid_from para o par de moedas.
    Optional<ExchangeRate> findFirstByBaseCurrencyAndQuoteCurrencyOrderByValidFromDesc(
            Currency baseCurrency, Currency quoteCurrency);
}
