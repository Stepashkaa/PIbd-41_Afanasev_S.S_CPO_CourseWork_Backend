package ru.kursach.kpo.tour_agency_backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tour_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        userRepository.deleteAll();
    }

    private String tokenFromResponse(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        String token = node.get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String signUpUserAndGetToken(String email) throws Exception {
        String body = """
          {"username":"UserTest1","email":"%s","password":"my_1secret1_password"}
        """.formatted(email);

        String resp = mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        return tokenFromResponse(resp);
    }

    private String signInAndGetToken(String email, String password) throws Exception {
        String body = """
          {"email":"%s","password":"%s"}
        """.formatted(email, password);

        String resp = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        return tokenFromResponse(resp);
    }

    @Test
    @DisplayName("GET /api/v1/cities без токена -> 401")
    void getAll_noToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/cities"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/cities с USER токеном -> 200")
    void getAll_user_ok() throws Exception {
        String token = signUpUserAndGetToken("user1@gmail.com");

        mockMvc.perform(get("/api/v1/cities")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/cities с USER токеном -> 403")
    void create_user_forbidden_403() throws Exception {
        String token = signUpUserAndGetToken("user2@gmail.com");

        String createCity = """
          {"name":"Riga","country":"Latvia","timezone":"Europe/Riga"}
        """;

        mockMvc.perform(post("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCity)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/cities с ADMIN токеном -> 201")
    void create_admin_created_201() throws Exception {
        String adminEmail = "admin@test.com";
        String rawPass = "Pa$sw0rd!";

        userRepository.save(UserEntity.builder()
                .username("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(rawPass))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());

        String adminToken = signInAndGetToken(adminEmail, rawPass);

        String createCity = """
          {"name":"Riga","country":"Latvia","timezone":"Europe/Riga"}
        """;

        mockMvc.perform(post("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createCity)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Riga"));
    }
}
