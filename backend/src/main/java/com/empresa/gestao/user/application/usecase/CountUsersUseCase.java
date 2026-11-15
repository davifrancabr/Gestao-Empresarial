package com.empresa.gestao.user.application.usecase;

import com.empresa.gestao.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CountUsersUseCase {

    private final UserRepository userRepository;

    public CountUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public long execute() {
        return userRepository.count();
    }
}
