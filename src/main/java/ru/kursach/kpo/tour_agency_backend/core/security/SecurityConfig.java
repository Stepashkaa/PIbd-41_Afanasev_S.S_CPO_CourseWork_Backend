package ru.kursach.kpo.tour_agency_backend.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import ru.kursach.kpo.tour_agency_backend.core.filter.JwtAuthenticationFilter;


import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfiguration = new CorsConfiguration();
                    corsConfiguration.setAllowedOriginPatterns(List.of("*"));
                    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setAllowCredentials(true);
                    return corsConfiguration;
                }))
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-resources/*", "/v3/api-docs/**").permitAll()

                        // Cities: читать могут все роли, CRUD — только ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/cities/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/cities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/cities/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cities/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/airports/**").hasRole("ADMIN")

                        // flights: подбор рейсов для привязок (ADMIN + MANAGER)
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/for-tour/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/for-departure/**").hasAnyRole("ADMIN", "MANAGER")

                        // flights: CRUD — только ADMIN
                        //.requestMatchers("/api/v1/flights/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/flights/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/flights/**").hasRole("ADMIN")
                        //.requestMatchers("/api/v1/flights/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/tours/public/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/v1/tours/my/**").hasRole("MANAGER")

                        // туры и вылеты туров — админ + менеджер
                        //.requestMatchers("/api/v1/tours/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/v1/tour-departures/**").hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/tours/*").hasAnyRole("ADMIN", "MANAGER")

                        // tours: CRUD — только ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/tours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tours/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/tours/**").hasRole("ADMIN")

                        // бронирования — обычные пользователи + менеджеры + админы
                        .requestMatchers("/api/v1/bookings/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/endpoint", "/admin/**").hasRole("ADMIN")

                        //.requestMatchers("/api/v1/public/**").permitAll()
                        //.requestMatchers("/api/v1/manager/**").hasRole("MANAGER")

                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

}
