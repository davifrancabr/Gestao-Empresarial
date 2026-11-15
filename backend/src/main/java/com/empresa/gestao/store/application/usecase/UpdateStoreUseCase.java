package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.dto.StoreCommand;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateStoreUseCase {

    private final StoreRepository storeRepository;

    public UpdateStoreUseCase(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreResult execute(UUID id, StoreCommand command) {
        Store current = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Loja não encontrada."));
        String name = cleanName(command.name());
        Store updated = new Store(current.id(), name, TextNormalizer.cleanText(command.document()),
                command.active() == null ? current.active() : command.active(),
                current.createdAt(), current.updatedAt());
        return StoreResultMapper.toResult(storeRepository.save(updated));
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new ConflictException("Informe o nome da loja.");
        }
        return name;
    }
}
