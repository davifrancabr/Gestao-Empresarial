package com.empresa.gestao.user.application.usecase;

import com.empresa.gestao.shared.domain.ConflictException;
import com.empresa.gestao.shared.domain.NotFoundException;
import com.empresa.gestao.shared.domain.util.TextNormalizer;
import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserCreateCommand;
import com.empresa.gestao.user.application.dto.UserResult;
import com.empresa.gestao.user.application.port.out.PasswordHasher;
import com.empresa.gestao.user.domain.model.User;
import com.empresa.gestao.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final StoreQueryPort storeQueryPort;
    private final PasswordHasher passwordHasher;

    public CreateUserUseCase(UserRepository userRepository, StoreQueryPort storeQueryPort,
                             PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.storeQueryPort = storeQueryPort;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public UserResult execute(UserCreateCommand command) {
        String email = normalizeEmail(command.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um usuário com este e-mail.");
        }
        List<UUID> storeIds = resolveStores(command.storeIds());
        User user = new User(null, cleanName(command.name()), email,
                passwordHasher.encode(command.password()), command.role(), true, storeIds, null, null);
        return UserResultMapper.toResult(userRepository.save(user), storeQueryPort);
    }

    private List<UUID> resolveStores(List<UUID> storeIds) {
        if (storeIds == null) {
            return List.of();
        }
        LinkedHashSet<UUID> unique = new LinkedHashSet<>(storeIds);
        for (UUID storeId : unique) {
            storeQueryPort.findStoreById(storeId)
                    .orElseThrow(() -> new NotFoundException("Loja não encontrada: " + storeId));
        }
        return List.copyOf(unique);
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
}
