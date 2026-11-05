package dev.jotaeme.branchhub.infrastructure.security;

import dev.jotaeme.branchhub.infrastructure.persistence.iam.IamUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class DbUserDetailsService implements UserDetailsService {
    private final IamUserRepository users;

    public DbUserDetailsService(IamUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("User not found");
        }
        var u = users.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean accountNonLocked = u.getLockedUntil() == null || u.getLockedUntil().isBefore(Instant.now());
        List<SimpleGrantedAuthority> authorities = u.getRoles().stream()
                .map(r -> r.getName())
                .distinct()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash() == null ? "" : u.getPasswordHash())
                .authorities(authorities)
                .accountLocked(!accountNonLocked)
                .disabled(!u.isEnabled())
                .build();
    }
}

