package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.CurrencyNotFoundException;
import com.srmasset.creditengine.persistence.entity.Currency;
import com.srmasset.creditengine.persistence.repository.CurrencyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyService {

	private final CurrencyRepository currencyRepository;

	public CurrencyService(CurrencyRepository currencyRepository) {
		this.currencyRepository = currencyRepository;
	}

	@Transactional
	public Currency create(String code, String name) {
		return currencyRepository.save(new Currency(code, name));
	}

	@Transactional(readOnly = true)
	public List<Currency> findAll() {
		return currencyRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Currency findById(Long id) {
		return currencyRepository.findById(id).orElseThrow(() -> new CurrencyNotFoundException(id));
	}

	@Transactional
	public Currency rename(Long id, String name) {
		Currency currency = findById(id);
		currency.rename(name);
		return currency;
	}

	// Moeda pode estar referenciada por receivables/exchange_rates/settlements
	// (FK), então "excluir" é desativar, não apagar a linha — o histórico e as
	// referências existentes continuam válidos, só bloqueia uso em novos
	// cadastros.
	@Transactional
	public void deactivate(Long id) {
		findById(id).deactivate();
	}

	@Transactional
	public void activate(Long id) {
		findById(id).activate();
	}
}
