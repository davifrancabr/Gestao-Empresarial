package com.empresa.gestao.auth;

import com.empresa.gestao.auth.dto.AuthSessionResponse;
import com.empresa.gestao.auth.dto.LoginRequest;
import com.empresa.gestao.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public AuthSessionResponse login(LoginRequest request, HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password()));

        // Proteção contra session fixation: descarta a sessão anônima anterior.
        HttpSession existingSession = httpRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return toSession((AppUserPrincipal) authentication.getPrincipal());
    }

    public AuthSessionResponse current(AppUserPrincipal principal) {
        return toSession(principal);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        SecurityContextHolder.clearContext();
    }

    private AuthSessionResponse toSession(AppUserPrincipal principal) {
        var user = new AuthSessionResponse.UserSummary(
                principal.id(), principal.name(), principal.email(), principal.role());
        var activeStoreId = principal.stores().isEmpty() ? null : principal.stores().getFirst().id();
        return new AuthSessionResponse(user, principal.stores(), activeStoreId);
    }
}
