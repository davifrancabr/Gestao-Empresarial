package com.empresa.gestao.store.application.port;

import com.empresa.gestao.store.application.dto.StoreResult;
import com.empresa.gestao.store.application.dto.StoreSummary;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta pública de leitura do módulo de lojas. Outros módulos validam e
 * enriquecem seus dados sem acessar repositórios ou entidades de persistência.
 */
public interface StoreQueryPort {

    Optional<StoreResult> findStoreById(UUID id);

    List<StoreResult> findActiveOrderByName();

    boolean existsActive(UUID id);

    List<StoreResult> findAllByIds(Collection<UUID> ids);

    List<StoreSummary> findSummariesByIds(Collection<UUID> ids);

    StoreResult getRequired(UUID id);
}
