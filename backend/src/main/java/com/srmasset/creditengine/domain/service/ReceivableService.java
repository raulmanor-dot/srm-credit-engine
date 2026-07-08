package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.AssignorNotFoundException;
import com.srmasset.creditengine.domain.exception.CurrencyNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableTypeNotFoundException;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.entity.Receivable;
import com.srmasset.creditengine.persistence.entity.ReceivableType;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableRepository;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceivableService {

	private final ReceivableRepository receivableRepository;
	private final AssignorRepository assignorRepository;
	private final ReceivableTypeRepository receivableTypeRepository;
	private final CurrencyRepository currencyRepository;

	public ReceivableService(
			ReceivableRepository receivableRepository,
			AssignorRepository assignorRepository,
			ReceivableTypeRepository receivableTypeRepository,
			CurrencyRepository currencyRepository) {
		this.receivableRepository = receivableRepository;
		this.assignorRepository = assignorRepository;
		this.receivableTypeRepository = receivableTypeRepository;
		this.currencyRepository = currencyRepository;
	}

	@Transactional
	public Receivable create(
			Long assignorId,
			Long receivableTypeId,
			Long faceValueCurrencyId,
			BigDecimal faceValue,
			String documentNumber,
			LocalDate issueDate,
			LocalDate dueDate) {
		Assignor assignor = assignorRepository.findById(assignorId)
				.orElseThrow(() -> new AssignorNotFoundException(assignorId));
		ReceivableType receivableType = receivableTypeRepository.findById(receivableTypeId)
				.orElseThrow(() -> new ReceivableTypeNotFoundException(receivableTypeId));
		Currency faceValueCurrency = currencyRepository.findById(faceValueCurrencyId)
				.orElseThrow(() -> new CurrencyNotFoundException(faceValueCurrencyId));

		Receivable receivable = new Receivable(
				assignor, receivableType, faceValueCurrency, faceValue, documentNumber, issueDate, dueDate);
		return receivableRepository.save(receivable);
	}

	@Transactional(readOnly = true)
	public List<Receivable> findAll(Receivable.Status status, Long assignorId) {
		if (status != null && assignorId != null) {
			return receivableRepository.findByStatusAndAssignorId(status, assignorId);
		}
		if (status != null) {
			return receivableRepository.findByStatus(status);
		}
		if (assignorId != null) {
			return receivableRepository.findByAssignorId(assignorId);
		}
		return receivableRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Receivable findById(Long id) {
		return receivableRepository.findById(id).orElseThrow(() -> new ReceivableNotFoundException(id));
	}

	@Transactional
	public Receivable update(Long id, BigDecimal faceValue, String documentNumber, LocalDate issueDate, LocalDate dueDate) {
		Receivable receivable = findById(id);
		receivable.amend(faceValue, documentNumber, issueDate, dueDate);
		return receivable;
	}

	@Transactional
	public void cancel(Long id) {
		findById(id).markAsCanceled();
	}
}
