package com.srmasset.creditengine.persistence.repository;

import com.srmasset.creditengine.persistence.entity.Receivable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

	List<Receivable> findByStatus(Receivable.Status status);

	List<Receivable> findByAssignorId(Long assignorId);

	List<Receivable> findByStatusAndAssignorId(Receivable.Status status, Long assignorId);
}
