package com.empresa.gestao.user.application.usecase;

import com.empresa.gestao.store.application.port.StoreQueryPort;
import com.empresa.gestao.user.application.dto.UserResult;
import com.empresa.gestao.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListUsersUseCase {

    private final UserRepository userRepository;
    private final StoreQueryPort storeQueryPort;

    public ListUsersUseCase(UserRepository userRepository, StoreQueryPort storeQueryPort) {
        this.userRepository = userRepository;
        this.storeQueryPort = storeQueryPort;
    }

    @Transactional(readOnly = true)
    public List<UserResult> execute() {
        return userRepository.findAllWithStores().stream()
                .map(user -> UserResultMapper.toResult(user, storeQueryPort))
                .toList();
    }
}
