package com.empresa.gestao.user.infrastructure.persistence.mapper;

import com.empresa.gestao.user.domain.model.User;
import com.empresa.gestao.user.infrastructure.persistence.entity.UserJpaEntity;
import com.empresa.gestao.user.infrastructure.persistence.entity.UserStoreJpaEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        List<UUID> storeIds = entity.getStoreLinks().stream()
                .map(UserStoreJpaEntity::getStoreId)
                .toList();
        return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getPasswordHash(),
                entity.getRole(), entity.isActive(), storeIds, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static UserJpaEntity toNewEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        applyFields(user, entity);
        for (UUID storeId : user.storeIds()) {
            entity.getStoreLinks().add(link(entity, storeId));
        }
        return entity;
    }

    public static void apply(User user, UserJpaEntity entity) {
        applyFields(user, entity);
        Set<UUID> target = new HashSet<>(user.storeIds());
        entity.getStoreLinks().removeIf(link -> !target.contains(link.getStoreId()));
        Set<UUID> current = new HashSet<>();
        entity.getStoreLinks().forEach(link -> current.add(link.getStoreId()));
        for (UUID storeId : user.storeIds()) {
            if (!current.contains(storeId)) {
                entity.getStoreLinks().add(link(entity, storeId));
            }
        }
    }

    private static void applyFields(User user, UserJpaEntity entity) {
        entity.setName(user.name());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setActive(user.active());
    }

    private static UserStoreJpaEntity link(UserJpaEntity user, UUID storeId) {
        UserStoreJpaEntity link = new UserStoreJpaEntity();
        link.setUser(user);
        link.setStoreId(storeId);
        return link;
    }
}
