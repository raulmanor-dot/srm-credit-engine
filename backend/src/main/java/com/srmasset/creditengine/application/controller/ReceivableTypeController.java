package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.ReceivableTypeResponse;
import com.srmasset.creditengine.persistence.repository.ReceivableTypeRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Sem Service: listagem de dado de referência somente leitura, sem regra de
// negócio a encapsular (nem findById, nem exceptions) — só usada para
// popular o seletor "Tipo" no formulário de novo recebível do frontend.
@RestController
@RequestMapping("/receivable-types")
public class ReceivableTypeController {

	private final ReceivableTypeRepository receivableTypeRepository;

	public ReceivableTypeController(ReceivableTypeRepository receivableTypeRepository) {
		this.receivableTypeRepository = receivableTypeRepository;
	}

	@GetMapping
	public List<ReceivableTypeResponse> findAll() {
		return receivableTypeRepository.findAll().stream().map(ReceivableTypeResponse::from).toList();
	}
}
