package za.co.patrick.ledgerplatform.application;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.patrick.ledgerplatform.infrastructure.AppUserEntity;
import za.co.patrick.ledgerplatform.infrastructure.AppUserEntityRepository;

@Service
public class PersistentUserDetailsService implements UserDetailsService {

    private final AppUserEntityRepository appUserRepository;

    public PersistentUserDetailsService(AppUserEntityRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUserEntity user = appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User %s was not found.".formatted(username)));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .toList())
                .build();
    }
}
