package com.empresa.gestao.auth.application.port;

import com.empresa.gestao.auth.application.dto.AuthSessionResult;

/**
 * Porta de autenticação. A implementação vive no adaptador de segurança e
 * concentra os detalhes de sessão, CSRF e Spring Security.
 */
public interface AuthSessionPort {

    AuthSessionResult authenticate(String email, String password);

    void logout();
}
