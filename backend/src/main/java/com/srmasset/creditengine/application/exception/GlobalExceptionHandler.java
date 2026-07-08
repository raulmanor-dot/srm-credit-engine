package com.srmasset.creditengine.application.exception;

import com.srmasset.creditengine.domain.exception.CurrencyNotFoundException;
import com.srmasset.creditengine.domain.exception.ExchangeRateNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotFoundException;
import com.srmasset.creditengine.domain.exception.ReceivableNotPendingException;
import com.srmasset.creditengine.domain.exception.UnsupportedReceivableTypeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({ReceivableNotFoundException.class, CurrencyNotFoundException.class})
	public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
	}

	@ExceptionHandler({UnsupportedReceivableTypeException.class, ExchangeRateNotFoundException.class})
	public ResponseEntity<ErrorResponse> handleUnprocessable(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
				.body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
	}

	// 409: o recebível já mudou de estado (liquidado/cancelado por outra
	// operação) — o próprio domínio detectou antes de qualquer escrita.
	@ExceptionHandler(ReceivableNotPendingException.class)
	public ResponseEntity<ErrorResponse> handleNotPending(ReceivableNotPendingException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
	}

	// 409: optimistic lock (receivables.version) — outra transação concorrente
	// liquidou o mesmo recebível entre o load e o save (ver ADR 0004).
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(HttpStatus.CONFLICT, "Concurrent update detected, please retry"));
	}

	// 409: defesa em profundidade — a constraint UNIQUE de settlements.receivable_id
	// pegou uma dupla liquidação que o optimistic lock não pegou a tempo.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of(HttpStatus.CONFLICT, "This operation conflicts with existing data"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Validation error");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(HttpStatus.BAD_REQUEST, message));
	}
}
