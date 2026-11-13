package com.empresa.gestao.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"storeLinks", "storeLinks.store"})
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findWithStoresByEmail(@Param("email") String email);

    @EntityGraph(attributePaths = {"storeLinks", "storeLinks.store"})
    @Query("select u from User u order by u.name")
    List<User> findAllWithStores();

    @EntityGraph(attributePaths = {"storeLinks", "storeLinks.store"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithStoresById(@Param("id") UUID id);
}
