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
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlightIntegrationTest {

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
    @Autowired CityRepository cityRepository;
    @Autowired AirportRepository airportRepository;
    @Autowired FlightRepository flightRepository;

    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        flightRepository.deleteAll();
        airportRepository.deleteAll();
        cityRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------------- helpers ----------------

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

    private String createAdminAndGetToken() throws Exception {
        String adminEmail = "admin@test.com";
        String rawPass = "Pa$sw0rd!";

        userRepository.save(UserEntity.builder()
                .username("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(rawPass))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());

        return signInAndGetToken(adminEmail, rawPass);
    }

    private CityEntity createCity(String name, String country) {
        return cityRepository.save(CityEntity.builder()
                .name(name)
                .country(country)
                .timezone("Europe/Riga")
                .build());
    }

    private AirportEntity createAirport(String iata, String name, CityEntity city) {
        return airportRepository.save(AirportEntity.builder()
                .iataCode(iata)
                .name(name)
                .city(city)
                .build());
    }

    private String createFlightBody(String flightNumber, AirportEntity dep, AirportEntity arr,
                                    LocalDateTime departAt, LocalDateTime arriveAt) {
        return """
          {
            "flightNumber":"%s",
            "carrier":"Aeroflot",
            "departAt":"%s",
            "arriveAt":"%s",
            "basePrice":%s,
            "departureAirportId":%d,
            "arrivalAirportId":%d
          }
        """.formatted(
                flightNumber,
                departAt,
                arriveAt,
                new BigDecimal("15000.00"),
                dep.getId(),
                arr.getId()
        );
    }

    // ---------------- tests ----------------

    @Test
    @DisplayName("GET /api/v1/flights без токена -> 403")
    void getAll_noToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/flights без токена -> 403")
    void create_noToken_403() throws Exception {
        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body = createFlightBody("SU100", dep, arr,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/flights с USER токеном -> 403 (если flights закрыты не для USER)")
    void create_userToken_403() throws Exception {
        String userToken = signUpUserAndGetToken("user1@gmail.com");

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body = createFlightBody("SU100", dep, arr,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/flights с ADMIN -> 201, затем GET /{id} -> 200")
    void create_admin_201_and_getById_200() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body = createFlightBody("SU100", dep, arr,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        String createdJson = mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.flightNumber").value("SU100"))
                .andExpect(jsonPath("$.departureAirportId").value(dep.getId()))
                .andExpect(jsonPath("$.arrivalAirportId").value(arr.getId()))
                .andReturn().getResponse().getContentAsString();

        long flightId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(get("/api/v1/flights/" + flightId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(flightId))
                .andExpect(jsonPath("$.carrier").value("Aeroflot"));
    }

    @Test
    @DisplayName("POST /api/v1/flights arriveAt < departAt -> 400")
    void create_arriveBeforeDepart_400() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body = createFlightBody("SU101", dep, arr,
                LocalDateTime.of(2026, 1, 10, 12, 0),
                LocalDateTime.of(2026, 1, 10, 10, 0) // arrive раньше
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/flights одинаковые аэропорты -> 400")
    void create_sameAirports_400() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);

        String body = createFlightBody("SU102", dep, dep,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/flights дубль flightNumber -> 400")
    void create_duplicateFlightNumber_400() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body1 = createFlightBody("SU200", dep, arr,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        String body2 = createFlightBody("SU200", dep, arr,
                LocalDateTime.of(2026, 1, 11, 10, 0),
                LocalDateTime.of(2026, 1, 11, 12, 0)
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/flights/search?number=... с ADMIN -> 200")
    void search_admin_200() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String body = createFlightBody("SU300", dep, arr,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/flights/search")
                        .param("number", "SU300")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value("SU300"));
    }

    @Test
    @DisplayName("GET /api/v1/flights/paged с ADMIN -> 200")
    void paged_admin_200() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        // 2 рейса
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightBody("SU401", dep, arr,
                                LocalDateTime.of(2026, 1, 10, 10, 0),
                                LocalDateTime.of(2026, 1, 10, 12, 0)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightBody("SU402", dep, arr,
                                LocalDateTime.of(2026, 1, 11, 10, 0),
                                LocalDateTime.of(2026, 1, 11, 12, 0)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/flights/paged")
                        .param("flightNumber", "SU4")
                        .param("departureAirportName", "Riga")
                        .param("arrivalAirportName", "Vilnius")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].flightNumber").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/flights/{id} с ADMIN -> 200")
    void update_admin_200() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String createdJson = mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightBody("SU500", dep, arr,
                                LocalDateTime.of(2026, 1, 10, 10, 0),
                                LocalDateTime.of(2026, 1, 10, 12, 0)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long flightId = objectMapper.readTree(createdJson).get("id").asLong();

        String updateBody = """
          {
            "flightNumber":"SU500",
            "carrier":"Aeroflot Updated",
            "departAt":"2026-01-10T11:00:00",
            "arriveAt":"2026-01-10T13:00:00",
            "status":"SCHEDULED",
            "basePrice":17000.00,
            "departureAirportId":%d,
            "arrivalAirportId":%d
          }
        """.formatted(dep.getId(), arr.getId());

        mockMvc.perform(put("/api/v1/flights/" + flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(flightId))
                .andExpect(jsonPath("$.carrier").value("Aeroflot Updated"))
                .andExpect(jsonPath("$.basePrice").value(17000.00));
    }

    @Test
    @DisplayName("DELETE /api/v1/flights/{id} с ADMIN -> 204, затем GET -> 404")
    void delete_admin_204() throws Exception {
        String adminToken = createAdminAndGetToken();

        CityEntity city = createCity("Riga", "Latvia");
        AirportEntity dep = createAirport("RIX", "Riga Airport", city);
        AirportEntity arr = createAirport("VNO", "Vilnius Airport", city);

        String createdJson = mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createFlightBody("SU600", dep, arr,
                                LocalDateTime.of(2026, 1, 10, 10, 0),
                                LocalDateTime.of(2026, 1, 10, 12, 0)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long flightId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(delete("/api/v1/flights/" + flightId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/flights/" + flightId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
