package ru.kursach.kpo.tour_agency_backend.core.setup;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntityInitializer {

    private static final Logger logger = LoggerFactory.getLogger(EntityInitializer.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.phone:}")
    private String adminPhone;

    @PostConstruct
    @Transactional
    public void initializeAll() {
        logger.info("Initializing database...");

        List<UserEntity> users = createUsers();
        if (users.isEmpty()) {
            logger.warn("No users created.");
        }

        logger.info("Database initialization completed.");
    }

    private List<UserEntity> createUsers() {
        List<UserEntity> users = new ArrayList<>();

        if (adminEmail == null || adminEmail.isBlank()) {
            logger.error("app.admin.email is not configured! Admin user cannot be created.");
            return users;
        }

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            logger.info("Admin user already exists: {}", adminEmail);
            return users;
        }

        UserEntity admin = UserEntity.builder()
                .username("admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .phone(adminPhone)
                .userRole(UserRole.ADMIN)
                .active(true)
                .build();

        users.add(userRepository.save(admin));
        logger.info("Admin user created successfully: {}", adminEmail);

        return users;
    }
}
