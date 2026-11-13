package com.empresa.gestao.user;

import com.empresa.gestao.common.exception.ConflictException;
import com.empresa.gestao.common.exception.ResourceNotFoundException;
import com.empresa.gestao.common.util.TextNormalizer;
import com.empresa.gestao.store.Store;
import com.empresa.gestao.store.StoreRepository;
import com.empresa.gestao.store.dto.StoreSummary;
import com.empresa.gestao.user.dto.UserCreateRequest;
import com.empresa.gestao.user.dto.UserResponse;
import com.empresa.gestao.user.dto.UserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, StoreRepository storeRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllWithStores().stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um usuário com este e-mail.");
        }
        User user = new User();
        user.setName(cleanName(request.name()));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        replaceStores(user, request.storeIds());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        User user = userRepository.findWithStoresById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        String email = normalizeEmail(request.email());
        userRepository.findWithStoresByEmail(email)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ConflictException("Já existe um usuário com este e-mail.");
                });

        user.setName(cleanName(request.name()));
        user.setEmail(email);
        user.setRole(request.role());
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        replaceStores(user, request.storeIds());
        return toResponse(userRepository.save(user));
    }

    private void replaceStores(User user, List<UUID> storeIds) {
        user.getStoreLinks().clear();
        if (storeIds == null) {
            return;
        }
        Set<UUID> unique = new HashSet<>(storeIds);
        for (UUID storeId : unique) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada: " + storeId));
            user.addStore(store);
        }
    }

    private String cleanName(String value) {
        String name = TextNormalizer.cleanText(value);
        if (name == null) {
            throw new ConflictException("Informe o nome do usuário.");
        }
        return name;
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        List<StoreSummary> stores = user.getStoreLinks().stream()
                .map(link -> new StoreSummary(link.getStore().getId(), link.getStore().getName()))
                .toList();
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isActive(), stores, user.getCreatedAt());
    }
}
