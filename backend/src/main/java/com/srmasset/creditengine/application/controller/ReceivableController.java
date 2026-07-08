package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.ReceivableRequest;
import com.srmasset.creditengine.application.dto.ReceivableResponse;
import com.srmasset.creditengine.application.dto.ReceivableUpdateRequest;
import com.srmasset.creditengine.domain.service.ReceivableService;
import com.srmasset.creditengine.persistence.entity.Receivable;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/receivables")
public class ReceivableController {

	private final ReceivableService receivableService;

	public ReceivableController(ReceivableService receivableService) {
		this.receivableService = receivableService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReceivableResponse create(@Valid @RequestBody ReceivableRequest request) {
		return ReceivableResponse.from(receivableService.create(
				request.assignorId(),
				request.receivableTypeId(),
				request.faceValueCurrencyId(),
				request.faceValue(),
				request.documentNumber(),
				request.issueDate(),
				request.dueDate()));
	}

	@GetMapping
	public List<ReceivableResponse> findAll(
			@RequestParam(required = false) Receivable.Status status, @RequestParam(required = false) Long assignorId) {
		return receivableService.findAll(status, assignorId).stream().map(ReceivableResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ReceivableResponse findById(@PathVariable Long id) {
		return ReceivableResponse.from(receivableService.findById(id));
	}

	// Só permitido enquanto o recebível está PENDING (ver Receivable.amend).
	@PutMapping("/{id}")
	public ReceivableResponse update(@PathVariable Long id, @Valid @RequestBody ReceivableUpdateRequest request) {
		return ReceivableResponse.from(receivableService.update(
				id, request.faceValue(), request.documentNumber(), request.issueDate(), request.dueDate()));
	}

	// "Excluir" um recebível é cancelá-lo (transição de estado), não apagar a
	// linha: preserva o histórico e é bloqueado se já SETTLED (ver
	// Receivable.markAsCanceled).
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(@PathVariable Long id) {
		receivableService.cancel(id);
		return ResponseEntity.noContent().build();
	}
}
