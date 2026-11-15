package com.empresa.gestao.user.application.port;

import com.empresa.gestao.user.application.dto.UserAuthView;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthQueryPort {

    Optional<UserAuthView> findAuthByEmail(String email);

    Optional<UserAuthView> findAuthById(UUID id);
}
