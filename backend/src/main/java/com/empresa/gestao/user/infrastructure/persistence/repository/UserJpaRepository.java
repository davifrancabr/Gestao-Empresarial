package com.empresa.gestao.user.infrastructure.persistence.repository;

import com.empresa.gestao.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"storeLinks"})
    @Query("select u from UserJpaEntity u where lower(u.email) = lower(:email)")
    Optional<UserJpaEntity> findWithStoresByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"storeLinks"})
    @Query("select u from UserJpaEntity u order by u.name")
    List<UserJpaEntity> findAllWithStores();

    @EntityGraph(attributePaths = {"storeLinks"})
    @Query("select u from UserJpaEntity u where u.id = :id")
    Optional<UserJpaEntity> findWithStoresById(@Param("id") UUID id);
}
