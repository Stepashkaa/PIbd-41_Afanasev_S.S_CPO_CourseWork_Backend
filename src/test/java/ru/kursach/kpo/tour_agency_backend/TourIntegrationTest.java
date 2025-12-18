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
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourDepartureRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourIntegrationTest {

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
    @Autowired PasswordEncoder passwordEncoder;

    @Autowired UserRepository userRepository;
    @Autowired CityRepository cityRepository;
    @Autowired TourRepository tourRepository;

    // если FK завязаны на departures/booking — удобно чистить и их (если есть репо)
    @Autowired(required = false) TourDepartureRepository tourDepartureRepository;

    @BeforeEach
    void clean() {
        // порядок важен из-за FK
        if (tourDepartureRepository != null) tourDepartureRepository.deleteAll();
        tourRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- helpers ----------

    private String tokenFrom(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        String token = node.get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private String signInAndGetToken(String email, String pass) throws Exception {
        String body = """
          {"email":"%s","password":"%s"}
        """.formatted(email, pass);

        String resp = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();

        return tokenFrom(resp);
    }

    private UserEntity createUser(String email, String rawPass, UserRole role) {
        return userRepository.save(UserEntity.builder()
                .username(email.split("@")[0])
                .email(email)
                .password(passwordEncoder.encode(rawPass))
                .userRole(role)
                .active(true)
                .build());
    }

    private String createAdminToken() throws Exception {
        createUser("admin@test.com", "Pa$sw0rd!", UserRole.ADMIN);
        return signInAndGetToken("admin@test.com", "Pa$sw0rd!");
    }

    private String createManagerToken(String email) throws Exception {
        createUser(email, "Pa$sw0rd!", UserRole.MANAGER);
        return signInAndGetToken(email, "Pa$sw0rd!");
    }

    private CityEntity city(String name) {
        return cityRepository.save(CityEntity.builder()
                .name(name)
                .country("Latvia")
                .timezone("Europe/Riga")
                .build());
    }

    private TourEntity tour(String title, CityEntity baseCity, UserEntity manager,
                            TourStatus status, boolean active, BigDecimal price) {
        return tourRepository.save(TourEntity.builder()
                .title(title)
                .description("desc")
                .durationDays(7)
                .basePrice(price != null ? price : new BigDecimal("1000.00"))
                .status(status)
                .active(active)
                .baseCity(baseCity)
                .managerUser(manager)
                .build());
    }

    // ---------- tests ----------

    @Test
    @DisplayName("GET /api/v1/tours/paged без токена -> 403")
    void toursPaged_noToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/tours/paged")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tours/public/paged: возвращает только PUBLISHED + active=true, фильтры работают")
    void publicPaged_filters_onlyPublishedAndActive() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        CityEntity vilnius = city("Vilnius");

        // должны попасть:
        tour("Riga Weekend", riga, null, TourStatus.PUBLISHED, true, new BigDecimal("2000.00"));
        tour("Riga City Break", riga, null, TourStatus.PUBLISHED, true, new BigDecimal("2100.00"));

        // не должны попасть:
        tour("Riga Draft", riga, null, TourStatus.DRAFT, true, new BigDecimal("1500.00"));
        tour("Riga Archived", riga, null, TourStatus.ARCHIVED, true, new BigDecimal("1500.00")); // хоть и active=true, но статус не PUBLISHED
        tour("Riga Inactive", riga, null, TourStatus.PUBLISHED, false, new BigDecimal("1500.00"));
        tour("Vilnius Weekend", vilnius, null, TourStatus.PUBLISHED, true, new BigDecimal("1900.00"));

        // фильтр: title содержит "Riga", baseCityId = Riga
        mockMvc.perform(get("/api/v1/tours/public/paged")
                        .param("title", "Riga")
                        .param("baseCityId", String.valueOf(riga.getId()))
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/tours/my/paged: MANAGER видит только свои туры")
    void myPaged_manager_onlyOwnTours() throws Exception {
        String m1Token = createManagerToken("m1@test.com");
        String m2Token = createManagerToken("m2@test.com");

        UserEntity m1 = userRepository.findByEmail("m1@test.com").orElseThrow();
        UserEntity m2 = userRepository.findByEmail("m2@test.com").orElseThrow();

        CityEntity riga = city("Riga");

        tour("M1 Tour 1", riga, m1, TourStatus.PUBLISHED, true, new BigDecimal("1000.00"));
        tour("M1 Tour 2", riga, m1, TourStatus.DRAFT, true, new BigDecimal("1200.00"));
        tour("M2 Tour 1", riga, m2, TourStatus.PUBLISHED, true, new BigDecimal("1300.00"));

        mockMvc.perform(get("/api/v1/tours/my/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].managerUserId").value(m1.getId()))
                .andExpect(jsonPath("$.content[1].managerUserId").value(m1.getId()));

        // для m2 — 1 тур
        mockMvc.perform(get("/api/v1/tours/my/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + m2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].managerUserId").value(m2.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/tours: ADMIN создаёт тур -> 201, возвращает нужные поля")
    void createTour_admin_created_201() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        UserEntity m1 = createUser("m1@test.com", "Pa$sw0rd!", UserRole.MANAGER);

        String body = """
        {
          "title": "Paris Weekend",
          "description": "Museums",
          "durationDays": 3,
          "basePrice": 45000.00,
          "active": true,
          "baseCityId": %d,
          "managerUserId": %d
        }
        """.formatted(riga.getId(), m1.getId());

        mockMvc.perform(post("/api/v1/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Paris Weekend"))
                .andExpect(jsonPath("$.baseCityId").value(riga.getId()))
                .andExpect(jsonPath("$.managerUserId").value(m1.getId()));
    }

    @Test
    @DisplayName("PUT /api/v1/tours/{id}: ARCHIVED + active=true -> 400 (валидация TourService)")
    void updateTour_archivedCantBeActive_400() throws Exception {
        String adminToken = createAdminToken();
        CityEntity riga = city("Riga");

        TourEntity t = tour("Test Tour", riga, null, TourStatus.PUBLISHED, true, new BigDecimal("1000.00"));

        String body = """
        {
          "title": "Test Tour",
          "description": "desc",
          "durationDays": 7,
          "basePrice": 1000.00,
          "status": "ARCHIVED",
          "active": true,
          "baseCityId": %d,
          "managerUserId": null
        }
        """.formatted(riga.getId());

        mockMvc.perform(put("/api/v1/tours/" + t.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/v1/tours/{id}: ADMIN удаляет -> 204, затем GET -> 404")
    void deleteTour_admin_noContent_thenNotFound() throws Exception {
        String adminToken = createAdminToken();
        CityEntity riga = city("Riga");

        TourEntity t = tour("To delete", riga, null, TourStatus.PUBLISHED, true, new BigDecimal("1000.00"));

        mockMvc.perform(delete("/api/v1/tours/" + t.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tours/" + t.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
