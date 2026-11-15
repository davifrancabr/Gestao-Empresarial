package com.empresa.gestao.store.application.usecase;

import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.dto.StoreCommand;
import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.domain.model.Store;
import com.empresa.gestao.store.domain.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateStoreUseCase {

    private final StoreRepository storeRepository;

    public CreateStoreUseCase(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreResult execute(StoreCommand command) {
        Store store = new Store(null, cleanName(command.name()),
                TextNormalizer.cleanText(command.document()), true, null, null);
        return StoreResultMapper.toResult(storeRepository.save(store));
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new ConflictException("Informe o nome da loja.");
        }
        return name;
    }
}
