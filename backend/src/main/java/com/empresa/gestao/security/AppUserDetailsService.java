package com.empresa.gestao.security;

import com.empresa.gestao.store.dto.StoreSummary;
import com.empresa.gestao.user.User;
import com.empresa.gestao.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findWithStoresByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas."));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public AppUserPrincipal loadById(UUID id) {
        User user = userRepository.findWithStoresById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
        return toPrincipal(user);
    }

    private AppUserPrincipal toPrincipal(User user) {
        List<StoreSummary> stores = user.getStoreLinks().stream()
                .map(link -> link.getStore())
                .filter(store -> store.isActive())
                .map(store -> new StoreSummary(store.getId(), store.getName()))
                .toList();
        return new AppUserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive(),
                stores
        );
    }
}
