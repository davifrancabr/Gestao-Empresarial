package com.empresa.gestao.shared.application.port;

import com.empresa.gestao.shared.application.dto.AuthenticatedUser;

/**
 * Identidade do usuário autenticado, sem dependência do mecanismo de sessão.
 */
public interface CurrentUserPort {

    AuthenticatedUser currentUser();
}
