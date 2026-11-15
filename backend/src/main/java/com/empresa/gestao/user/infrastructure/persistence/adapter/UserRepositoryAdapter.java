package com.empresa.gestao.user.infrastructure.persistence.adapter;

import com.empresa.gestao.store.application.dto.StoreSummary;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserAuthView;
import com.empresa.gestao.user.application.dto.UserInfo;
import com.empresa.gestao.user.application.port.UserAuthQueryPort;
import com.empresa.gestao.user.application.port.UserQueryPort;
import com.empresa.gestao.user.domain.model.User;
import com.empresa.gestao.user.domain.repository.UserRepository;
import com.empresa.gestao.user.infrastructure.persistence.entity.UserJpaEntity;
import com.empresa.gestao.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.empresa.gestao.user.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepository, UserQueryPort, UserAuthQueryPort {

    private final UserJpaRepository repository;
    private final StoreQueryPort storeQueryPort;

    public UserRepositoryAdapter(UserJpaRepository repository, StoreQueryPort storeQueryPort) {
        this.repository = repository;
        this.storeQueryPort = storeQueryPort;
    }

    @Override
    public Optional<User> findWithStoresByEmail(String email) {
        return repository.findWithStoresByEmail(email).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findWithStoresById(UUID id) {
        return repository.findWithStoresById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public List<User> findAllWithStores() {
        return repository.findAllWithStores().stream().map(UserPersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = user.id() == null
                ? UserPersistenceMapper.toNewEntity(user)
                : repository.findWithStoresById(user.id())
                        .map(existing -> {
                            UserPersistenceMapper.apply(user, existing);
                            return existing;
                        })
                        .orElseGet(() -> UserPersistenceMapper.toNewEntity(user));
        return UserPersistenceMapper.toDomain(repository.save(entity));
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Optional<UserInfo> findById(UUID id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain).map(this::toInfo);
    }

    @Override
    public Optional<UserInfo> findByEmail(String email) {
        return repository.findWithStoresByEmail(email)
                .map(UserPersistenceMapper::toDomain)
                .map(this::toInfo);
    }

    @Override
    public Map<UUID, String> findNamesByIds(Collection<UUID> ids) {
        Map<UUID, String> names = new LinkedHashMap<>();
        repository.findAllById(ids).forEach(entity -> names.put(entity.getId(), entity.getName()));
        return names;
    }

    @Override
    public Optional<UserAuthView> findAuthByEmail(String email) {
        return repository.findWithStoresByEmail(email)
                .map(UserPersistenceMapper::toDomain)
                .map(this::toAuthView);
    }

    @Override
    public Optional<UserAuthView> findAuthById(UUID id) {
        return repository.findWithStoresById(id)
                .map(UserPersistenceMapper::toDomain)
                .map(this::toAuthView);
    }

    private UserInfo toInfo(User user) {
        return new UserInfo(user.id(), user.name(), user.email(), user.role(), user.active());
    }

    private UserAuthView toAuthView(User user) {
        List<StoreSummary> stores = user.storeIds().stream()
                .map(storeQueryPort::findStoreById)
                .flatMap(java.util.Optional::stream)
                .filter(store -> store.active())
                .map(store -> new StoreSummary(store.id(), store.name()))
                .toList();
        return new UserAuthView(user.id(), user.name(), user.email(), user.passwordHash(),
                user.role(), user.active(), stores);
    }
}
