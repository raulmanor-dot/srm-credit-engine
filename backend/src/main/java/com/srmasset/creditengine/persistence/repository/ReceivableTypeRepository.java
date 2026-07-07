package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.domain.pricing.ReceivableTypeCode;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableTypeRepository extends JpaRepository<ReceivableType, Long> {

	Optional<ReceivableType> findByCode(ReceivableTypeCode code);
}
