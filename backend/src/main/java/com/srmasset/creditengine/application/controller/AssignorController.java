package com.srmasset.creditengine.application.controller;

import com.srmasset.creditengine.application.dto.AssignorRequest;
import com.srmasset.creditengine.application.dto.AssignorResponse;
import com.srmasset.creditengine.application.dto.AssignorUpdateRequest;
import com.srmasset.creditengine.domain.service.AssignorService;
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
@RequestMapping("/assignors")
public class AssignorController {

    private final AssignorService assignorService;

    public AssignorController(AssignorService assignorService) {
        this.assignorService = assignorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignorResponse create(@Valid @RequestBody AssignorRequest request) {
        return AssignorResponse.from(assignorService.create(request.name(), request.taxId()));
    }

    @GetMapping
    public List<AssignorResponse> findAll() {
        return assignorService.findAll().stream().map(AssignorResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AssignorResponse findById(@PathVariable Long id) {
        return AssignorResponse.from(assignorService.findById(id));
    }

    @PutMapping("/{id}")
    public AssignorResponse update(
            @PathVariable Long id, @Valid @RequestBody AssignorUpdateRequest request) {
        return AssignorResponse.from(assignorService.rename(id, request.name()));
    }

    // Soft delete: ver AssignorService.deactivate.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        assignorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        assignorService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
