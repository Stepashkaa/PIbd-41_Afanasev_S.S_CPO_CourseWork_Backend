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
import ru.kursach.kpo.tour_agency_backend.model.entity.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourDepartureIntegrationTest {

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
    @Autowired TourDepartureRepository tourDepartureRepository;

    // (flight/airport репо здесь не нужно — мы тестим departure без привязки рейсов)
    @BeforeEach
    void clean() {
        tourDepartureRepository.deleteAll();
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

    private TourEntity tour(String title, CityEntity baseCity, UserEntity manager, BigDecimal basePrice) {
        return tourRepository.save(TourEntity.builder()
                .title(title)
                .description("desc")
                .durationDays(7)
                .basePrice(basePrice)
                .status(TourStatus.PUBLISHED)
                .active(true)
                .baseCity(baseCity)
                .managerUser(manager)
                .build());
    }

    private TourDepartureEntity departure(TourEntity tour, LocalDate start, LocalDate end, TourDepartureStatus status) {
        return tourDepartureRepository.save(TourDepartureEntity.builder()
                .tour(tour)
                .startDate(start)
                .endDate(end)
                .capacityTotal(30)
                .capacityReserved(0)
                .priceOverride(null)
                .status(status != null ? status : TourDepartureStatus.PLANNED)
                .build());
    }

    // ---------- tests ----------

    @Test
    @DisplayName("GET /api/v1/tour-departures/paged без токена -> 403")
    void departuresPaged_noToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/tour-departures/paged")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/tour-departures/paged: startFrom > startTo -> 400")
    void departuresPaged_invalidDateRange_400() throws Exception {
        String adminToken = createAdminToken();

        mockMvc.perform(get("/api/v1/tour-departures/paged")
                        .param("startFrom", "2026-01-10")
                        .param("startTo", "2026-01-05")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/tour-departures/paged: фильтр по tourId и status работает")
    void departuresPaged_filtersByTourAndStatus() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        TourEntity t1 = tour("T1", riga, null, new BigDecimal("1000.00"));
        TourEntity t2 = tour("T2", riga, null, new BigDecimal("1000.00"));

        departure(t1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);
        departure(t1, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 12), TourDepartureStatus.CANCELLED);
        departure(t2, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);

        mockMvc.perform(get("/api/v1/tour-departures/paged")
                        .param("tourId", String.valueOf(t1.getId()))
                        .param("status", "PLANNED")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tourId").value(t1.getId()))
                .andExpect(jsonPath("$.content[0].status").value("PLANNED"));
    }

    @Test
    @DisplayName("GET /api/v1/tour-departures/my/paged: MANAGER видит только свои вылеты")
    void myPaged_manager_onlyOwnDepartures() throws Exception {
        String m1Token = createManagerToken("m1@test.com");
        String m2Token = createManagerToken("m2@test.com");

        UserEntity m1 = userRepository.findByEmail("m1@test.com").orElseThrow();
        UserEntity m2 = userRepository.findByEmail("m2@test.com").orElseThrow();

        CityEntity riga = city("Riga");

        TourEntity tM1 = tour("M1 Tour", riga, m1, new BigDecimal("1000.00"));
        TourEntity tM2 = tour("M2 Tour", riga, m2, new BigDecimal("1000.00"));

        departure(tM1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);
        departure(tM1, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 12), TourDepartureStatus.PLANNED);
        departure(tM2, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);

        mockMvc.perform(get("/api/v1/tour-departures/my/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/tour-departures/my/paged")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + m2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/tour-departures: MANAGER может создать вылет только для своего тура (иначе 403)")
    void create_manager_ownsTour_ok_and_forbidden() throws Exception {
        String m1Token = createManagerToken("m1@test.com");
        String m2Token = createManagerToken("m2@test.com");

        UserEntity m1 = userRepository.findByEmail("m1@test.com").orElseThrow();
        UserEntity m2 = userRepository.findByEmail("m2@test.com").orElseThrow();

        CityEntity riga = city("Riga");

        TourEntity tM1 = tour("M1 Tour", riga, m1, new BigDecimal("1000.00"));
        TourEntity tM2 = tour("M2 Tour", riga, m2, new BigDecimal("1000.00"));

        String bodyForM1Tour = """
        {
          "startDate": "2026-01-10",
          "endDate": "2026-01-12",
          "capacityTotal": 30,
          "capacityReserved": 0,
          "priceOverride": 900.00,
          "tourId": %d,
          "flightIds": []
        }
        """.formatted(tM1.getId());

        // m1 -> OK
        mockMvc.perform(post("/api/v1/tour-departures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyForM1Tour)
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tourId").value(tM1.getId()));

        // m1 пытается создать для тура m2 -> 403
        String bodyForM2Tour = bodyForM1Tour.replace(String.valueOf(tM1.getId()), String.valueOf(tM2.getId()));
        mockMvc.perform(post("/api/v1/tour-departures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyForM2Tour)
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/tour-departures: endDate < startDate -> 400")
    void create_invalidDates_400() throws Exception {
        String adminToken = createAdminToken();
        CityEntity riga = city("Riga");
        TourEntity t = tour("T1", riga, null, new BigDecimal("1000.00"));

        String body = """
        {
          "startDate": "2026-01-12",
          "endDate": "2026-01-10",
          "capacityTotal": 30,
          "capacityReserved": 0,
          "priceOverride": 900.00,
          "tourId": %d,
          "flightIds": []
        }
        """.formatted(t.getId());

        mockMvc.perform(post("/api/v1/tour-departures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/tour-departures: capacityReserved > capacityTotal -> 400")
    void create_invalidCapacity_400() throws Exception {
        String adminToken = createAdminToken();
        CityEntity riga = city("Riga");
        TourEntity t = tour("T1", riga, null, new BigDecimal("1000.00"));

        String body = """
        {
          "startDate": "2026-01-10",
          "endDate": "2026-01-12",
          "capacityTotal": 10,
          "capacityReserved": 11,
          "priceOverride": 900.00,
          "tourId": %d,
          "flightIds": []
        }
        """.formatted(t.getId());

        mockMvc.perform(post("/api/v1/tour-departures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/tour-departures: priceOverride >= basePrice -> 400")
    void create_priceOverrideMustBeLessThanBasePrice_400() throws Exception {
        String adminToken = createAdminToken();
        CityEntity riga = city("Riga");
        TourEntity t = tour("T1", riga, null, new BigDecimal("1000.00"));

        String body = """
        {
          "startDate": "2026-01-10",
          "endDate": "2026-01-12",
          "capacityTotal": 30,
          "capacityReserved": 0,
          "priceOverride": 1000.00,
          "tourId": %d,
          "flightIds": []
        }
        """.formatted(t.getId());

        mockMvc.perform(post("/api/v1/tour-departures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/tour-departures/{id}: MANAGER редактирует только свой вылет (иначе 403)")
    void update_manager_ownership_ok_and_forbidden() throws Exception {
        String m1Token = createManagerToken("m1@test.com");
        String m2Token = createManagerToken("m2@test.com");

        UserEntity m1 = userRepository.findByEmail("m1@test.com").orElseThrow();
        UserEntity m2 = userRepository.findByEmail("m2@test.com").orElseThrow();

        CityEntity riga = city("Riga");
        TourEntity tM1 = tour("M1 Tour", riga, m1, new BigDecimal("1000.00"));
        TourEntity tM2 = tour("M2 Tour", riga, m2, new BigDecimal("1000.00"));

        TourDepartureEntity d1 = departure(tM1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);

        String updateBody = """
        {
          "startDate": "2026-01-11",
          "endDate": "2026-01-13",
          "capacityTotal": 30,
          "capacityReserved": 0,
          "priceOverride": 900.00,
          "status": "PLANNED",
          "tourId": %d,
          "flightIds": []
        }
        """.formatted(tM1.getId());

        // m1 -> OK
        mockMvc.perform(put("/api/v1/tour-departures/" + d1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(d1.getId()))
                .andExpect(jsonPath("$.startDate").value("2026-01-11"));

        // m2 -> forbidden (чужой вылет)
        mockMvc.perform(put("/api/v1/tour-departures/" + d1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody.replace(String.valueOf(tM1.getId()), String.valueOf(tM2.getId())))
                        .header("Authorization", "Bearer " + m2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/tour-departures/{id}: MANAGER удаляет свой вылет -> 204, чужой -> 403")
    void delete_manager_ownership_ok_and_forbidden() throws Exception {
        String m1Token = createManagerToken("m1@test.com");
        String m2Token = createManagerToken("m2@test.com");

        UserEntity m1 = userRepository.findByEmail("m1@test.com").orElseThrow();
        UserEntity m2 = userRepository.findByEmail("m2@test.com").orElseThrow();

        CityEntity riga = city("Riga");
        TourEntity tM1 = tour("M1 Tour", riga, m1, new BigDecimal("1000.00"));
        TourEntity tM2 = tour("M2 Tour", riga, m2, new BigDecimal("1000.00"));

        TourDepartureEntity d1 = departure(tM1, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), TourDepartureStatus.PLANNED);

        // чужой менеджер -> 403
        mockMvc.perform(delete("/api/v1/tour-departures/" + d1.getId())
                        .header("Authorization", "Bearer " + m2Token))
                .andExpect(status().isForbidden());

        // владелец -> 204
        mockMvc.perform(delete("/api/v1/tour-departures/" + d1.getId())
                        .header("Authorization", "Bearer " + m1Token))
                .andExpect(status().isNoContent());
    }
}