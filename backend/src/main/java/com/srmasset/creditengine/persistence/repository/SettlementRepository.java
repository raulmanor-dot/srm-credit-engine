package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.persistence.entity.Settlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	Optional<Settlement> findByReceivableId(Long receivableId);
}
