package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.persistence.entity.Currency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    Optional<Currency> findByCode(String code);
}
