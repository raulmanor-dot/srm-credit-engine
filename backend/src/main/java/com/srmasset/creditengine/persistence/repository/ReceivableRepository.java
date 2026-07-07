package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.persistence.entity.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {
}
