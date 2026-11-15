package com.empresa.gestao.user.application.port;

import com.empresa.gestao.user.application.dto.UserInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserQueryPort {

    Optional<UserInfo> findById(UUID id);

    Optional<UserInfo> findByEmail(String email);

    Map<UUID, String> findNamesByIds(Collection<UUID> ids);
}
