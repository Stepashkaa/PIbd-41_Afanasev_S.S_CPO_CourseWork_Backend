package ru.kursach.kpo.tour_agency_backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.kursach.kpo.tour_agency_backend.dto.JwtAuthenticationResponse;
import ru.kursach.kpo.tour_agency_backend.dto.SignInRequest;
import ru.kursach.kpo.tour_agency_backend.dto.SignUpRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailService customUserDetailService;
    private final JwtService jwtService;

    public JwtAuthenticationResponse signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Пользователь с таким email уже существует");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .userRole(UserRole.USER)
                .active(true)
                .build();

        userRepository.save(user);
        UserDetails userDetails = customUserDetailService.loadUserByUsername(user.getEmail());
        String jwt = jwtService.generateToken(userDetails);

        return JwtAuthenticationResponse.builder()
                .token(jwt)
                .build();
    }

    public JwtAuthenticationResponse signIn(SignInRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserDetails userDetails =
                customUserDetailService.loadUserByUsername(request.email());

        String jwt = jwtService.generateToken(userDetails);

        return JwtAuthenticationResponse.builder()
                .token(jwt)
                .build();
    }
}
