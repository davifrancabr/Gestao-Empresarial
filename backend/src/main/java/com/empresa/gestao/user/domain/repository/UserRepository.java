package com.empresa.gestao.user.domain.repository;

import com.empresa.gestao.user.domain.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findWithStoresByEmail(String email);

    Optional<User> findWithStoresById(UUID id);

    List<User> findAllWithStores();

    boolean existsByEmailIgnoreCase(String email);

    User save(User user);

    long count();
}
