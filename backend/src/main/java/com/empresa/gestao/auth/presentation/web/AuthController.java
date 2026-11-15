package com.empresa.gestao.auth.presentation.web;

import com.empresa.gestao.auth.application.dto.AuthSessionResult;
import com.empresa.gestao.auth.application.dto.LoginCommand;
import com.empresa.gestao.auth.application.usecase.CurrentSessionUseCase;
import com.empresa.gestao.auth.application.usecase.LoginUseCase;
import com.empresa.gestao.auth.application.usecase.LogoutUseCase;
import com.empresa.gestao.auth.presentation.web.request.LoginRequest;
import com.empresa.gestao.auth.presentation.web.response.CsrfResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final CurrentSessionUseCase currentSessionUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(LoginUseCase loginUseCase, CurrentSessionUseCase currentSessionUseCase,
                          LogoutUseCase logoutUseCase) {
        this.loginUseCase = loginUseCase;
        this.currentSessionUseCase = currentSessionUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @PostMapping("/login")
    public AuthSessionResult login(@Valid @RequestBody LoginRequest request) {
        return loginUseCase.execute(new LoginCommand(request.email(), request.password()));
    }

    @GetMapping("/me")
    public AuthSessionResult me() {
        return currentSessionUseCase.execute();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        logoutUseCase.execute();
    }
}
