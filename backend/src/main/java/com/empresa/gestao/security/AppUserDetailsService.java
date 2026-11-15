package com.empresa.gestao.security;

import com.empresa.gestao.user.application.dto.UserAuthView;
import com.empresa.gestao.user.application.port.UserAuthQueryPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAuthQueryPort userAuthQueryPort;

    public AppUserDetailsService(UserAuthQueryPort userAuthQueryPort) {
        this.userAuthQueryPort = userAuthQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthView user = userAuthQueryPort.findAuthByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public AppUserPrincipal loadById(UUID id) {
        UserAuthView user = userAuthQueryPort.findAuthById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return toPrincipal(user);
    }

    private AppUserPrincipal toPrincipal(UserAuthView user) {
        return new AppUserPrincipal(
                user.id(),
                user.name(),
                user.email(),
                user.passwordHash(),
                user.role(),
                user.active(),
                user.stores()
        );
    }
}
