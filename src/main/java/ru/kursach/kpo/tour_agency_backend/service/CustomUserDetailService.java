package ru.kursach.kpo.tour_agency_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (!user.isActive()) {
                        throw new UsernameNotFoundException("Аккаунт деактивирован");
                    }

                    return User.builder()
                            .username(user.getEmail())
                            .password(user.getPassword())
                            .authorities(Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + user.getUserRole().name())
                            ))
                            .accountExpired(false)
                            .accountLocked(!user.isActive())
                            .credentialsExpired(false)
                            .disabled(!user.isActive())
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
