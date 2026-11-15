package com.empresa.gestao.auth.application.usecase;

import com.empresa.gestao.auth.application.dto.AuthSessionResult;
import com.empresa.gestao.auth.application.dto.LoginCommand;
import com.empresa.gestao.auth.application.port.AuthSessionPort;
import org.springframework.stereotype.Service;

@Service
public class LoginUseCase {

    private final AuthSessionPort authSessionPort;

    public LoginUseCase(AuthSessionPort authSessionPort) {
        this.authSessionPort = authSessionPort;
    }

    public AuthSessionResult execute(LoginCommand command) {
        return authSessionPort.authenticate(command.email(), command.password());
    }
}
