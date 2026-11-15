package com.empresa.gestao.user.application.dto;

import com.empresa.gestao.shared.domain.model.Role;

import java.util.UUID;

public record UserInfo(UUID id, String name, String email, Role role, boolean active) {
}
