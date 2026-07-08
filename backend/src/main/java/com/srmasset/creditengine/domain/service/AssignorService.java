package com.srmasset.creditengine.domain.service;

import com.srmasset.creditengine.domain.exception.AssignorNotFoundException;
import com.srmasset.creditengine.persistence.entity.Assignor;
import com.srmasset.creditengine.persistence.repository.AssignorRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignorService {

    private final AssignorRepository assignorRepository;

    public AssignorService(AssignorRepository assignorRepository) {
        this.assignorRepository = assignorRepository;
    }

    @Transactional
    public Assignor create(String name, String taxId) {
        return assignorRepository.save(new Assignor(name, taxId));
    }

    @Transactional(readOnly = true)
    public List<Assignor> findAll() {
        return assignorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Assignor findById(Long id) {
        return assignorRepository.findById(id).orElseThrow(() -> new AssignorNotFoundException(id));
    }

    @Transactional
    public Assignor rename(Long id, String name) {
        Assignor assignor = findById(id);
        assignor.rename(name);
        return assignor;
    }

    // Cedente pode ter recebíveis vinculados (FK), então "excluir" é desativar,
    // não apagar a linha.
    @Transactional
    public void deactivate(Long id) {
        findById(id).deactivate();
    }

    @Transactional
    public void activate(Long id) {
        findById(id).activate();
    }
}
