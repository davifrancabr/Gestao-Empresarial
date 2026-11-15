package com.empresa.gestao.auth.application.usecase;

import com.empresa.gestao.auth.application.port.AuthSessionPort;
import org.springframework.stereotype.Service;

@Service
public class LogoutUseCase {

    private final AuthSessionPort authSessionPort;

    public LogoutUseCase(AuthSessionPort authSessionPort) {
        this.authSessionPort = authSessionPort;
    }

    public void execute() {
        authSessionPort.logout();
    }
}
