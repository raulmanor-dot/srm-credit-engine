package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.CurrencyRequest;
import com.srmasset.creditengine.application.dto.CurrencyResponse;
import com.srmasset.creditengine.application.dto.CurrencyUpdateRequest;
import com.srmasset.creditengine.domain.service.CurrencyService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/currencies")
public class CurrencyController {

	private final CurrencyService currencyService;

	public CurrencyController(CurrencyService currencyService) {
		this.currencyService = currencyService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CurrencyResponse create(@Valid @RequestBody CurrencyRequest request) {
		return CurrencyResponse.from(currencyService.create(request.code(), request.name()));
	}

	@GetMapping
	public List<CurrencyResponse> findAll() {
		return currencyService.findAll().stream().map(CurrencyResponse::from).toList();
	}

	@GetMapping("/{id}")
	public CurrencyResponse findById(@PathVariable Long id) {
		return CurrencyResponse.from(currencyService.findById(id));
	}

	@PutMapping("/{id}")
	public CurrencyResponse update(@PathVariable Long id, @Valid @RequestBody CurrencyUpdateRequest request) {
		return CurrencyResponse.from(currencyService.rename(id, request.name()));
	}

	// Soft delete: ver CurrencyService.deactivate.
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		currencyService.deactivate(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/activate")
	public ResponseEntity<Void> activate(@PathVariable Long id) {
		currencyService.activate(id);
		return ResponseEntity.noContent().build();
	}
}
