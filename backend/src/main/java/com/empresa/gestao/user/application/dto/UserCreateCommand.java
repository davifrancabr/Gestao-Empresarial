package com.empresa.gestao.user.application.dto;

import com.empresa.gestao.shared.domain.model.Role;

import java.util.List;
import java.util.UUID;

public record UserCreateCommand(
        String name,
        String email,
        String password,
        Role role,
        List<UUID> storeIds
) {
}
