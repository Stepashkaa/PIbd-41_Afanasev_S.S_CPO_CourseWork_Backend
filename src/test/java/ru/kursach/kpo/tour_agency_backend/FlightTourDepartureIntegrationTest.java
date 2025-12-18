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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlightTourDepartureIntegrationTest {

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
    @Autowired AirportRepository airportRepository;
    @Autowired TourRepository tourRepository;
    @Autowired TourDepartureRepository tourDepartureRepository;
    @Autowired FlightRepository flightRepository;

    @BeforeEach
    void clean() {
        // порядок важен из-за FK
        flightRepository.deleteAll();
        tourDepartureRepository.deleteAll();
        tourRepository.deleteAll();
        airportRepository.deleteAll();
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
                .username(role.name().toLowerCase())
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

    private AirportEntity airport(String iata, String name, CityEntity city) {
        return airportRepository.save(AirportEntity.builder()
                .iataCode(iata)
                .name(name)
                .city(city)
                .build());
    }

    private TourEntity tour(String title, CityEntity baseCity, UserEntity manager) {
        return tourRepository.save(TourEntity.builder()
                .title(title)
                .description("desc")
                .durationDays(7)
                .basePrice(new BigDecimal("1000.00"))
                .status(TourStatus.PUBLISHED)
                .active(true)
                .baseCity(baseCity)
                .managerUser(manager)
                .build());
    }

    private TourDepartureEntity departure(TourEntity tour, LocalDate start, LocalDate end) {
        return tourDepartureRepository.save(TourDepartureEntity.builder()
                .tour(tour)
                .startDate(start)
                .endDate(end)
                .capacityTotal(30)
                .capacityReserved(0)
                .priceOverride(null)
                .status(TourDepartureStatus.PLANNED)
                .build());
    }

    private FlightEntity flight(String number, AirportEntity dep, AirportEntity arr, LocalDateTime departAt, LocalDateTime arriveAt) {
        return flightRepository.save(FlightEntity.builder()
                .flightNumber(number)
                .carrier("Aeroflot")
                .departAt(departAt)
                .arriveAt(arriveAt)
                .basePrice(new BigDecimal("15000.00"))
                .departureAirport(dep)
                .arrivalAirport(arr)
                .build());
    }

    // ---------- tests ----------

    @Test
    @DisplayName("GET /api/v1/flights/for-tour/{tourId} без токена -> 403")
    void flightsForTour_noToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/flights/for-tour/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/flights/for-tour/{tourId} с ADMIN -> 200 (фильтр по базовому городу)")
    void flightsForTour_admin_ok_filtersByBaseCity() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        CityEntity vilnius = city("Vilnius");

        AirportEntity rix = airport("RIX", "Riga Airport", riga);
        AirportEntity vno = airport("VNO", "Vilnius Airport", vilnius);
        AirportEntity kyy = airport("KYY", "Kaunas Airport", vilnius);

        // тур с baseCity = Riga
        TourEntity tour = tour("Riga Tour", riga, null);

        // рейсы: один подходит (город вылета/прилёта = Riga), другой нет
        flight("SU100", rix, vno,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0));

        flight("SU200", vno, kyy,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0));

        mockMvc.perform(get("/api/v1/flights/for-tour/" + tour.getId())
                        .param("flightNumber", "SU")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                // должен вернуться только SU100
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].flightNumber").value("SU100"));
    }

    @Test
    @DisplayName("GET /api/v1/flights/for-tour-departure/{tourId}?startDate&endDate с ADMIN -> 200 (даты пересекаются)")
    void flightsForTourDeparture_admin_ok_dateRange() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        CityEntity vilnius = city("Vilnius");
        AirportEntity rix = airport("RIX", "Riga Airport", riga);
        AirportEntity vno = airport("VNO", "Vilnius Airport", vilnius);

        TourEntity tour = tour("Riga Tour", riga, null);

        // В тесте проверяем диапазон дат: 2026-01-10..2026-01-12
        flight("SU101", rix, vno,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)); // подходит

        flight("SU102", rix, vno,
                LocalDateTime.of(2026, 1, 13, 10, 0),
                LocalDateTime.of(2026, 1, 13, 12, 0)); // не подходит

        mockMvc.perform(get("/api/v1/flights/for-tour-departure/" + tour.getId())
                        .param("startDate", "2026-01-10")
                        .param("endDate", "2026-01-12")
                        .param("flightNumber", "SU")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].flightNumber").value("SU101"));
    }

    @Test
    @DisplayName("GET /api/v1/flights/for-departure/{departureId} с ADMIN -> 200 (фильтр по датам вылета тура)")
    void flightsForDeparture_admin_ok_intersection() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        CityEntity vilnius = city("Vilnius");
        AirportEntity rix = airport("RIX", "Riga Airport", riga);
        AirportEntity vno = airport("VNO", "Vilnius Airport", vilnius);

        TourEntity tour = tour("Riga Tour", riga, null);
        TourDepartureEntity dep = departure(tour, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12));

        // рейс пересекается по датам -> должен попасть
        flight("SU301", rix, vno,
                LocalDateTime.of(2026, 1, 11, 10, 0),
                LocalDateTime.of(2026, 1, 11, 12, 0));

        // рейс не пересекается -> не должен попасть
        flight("SU302", rix, vno,
                LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 1, 20, 12, 0));

        mockMvc.perform(get("/api/v1/flights/for-departure/" + dep.getId())
                        .param("flightNumber", "SU")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].flightNumber").value("SU301"));
    }

    @Test
    @DisplayName("POST/DELETE привязки: ADMIN может привязать/отвязать любой вылет")
    void addAndRemoveDeparture_admin_ok() throws Exception {
        String adminToken = createAdminToken();

        CityEntity riga = city("Riga");
        CityEntity vilnius = city("Vilnius");
        AirportEntity rix = airport("RIX", "Riga Airport", riga);
        AirportEntity vno = airport("VNO", "Vilnius Airport", vilnius);

        TourEntity tour = tour("Admin Tour", riga, null);
        TourDepartureEntity dep = departure(tour, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12));

        FlightEntity f = flight("SU910", rix, vno,
                LocalDateTime.of(2026, 1, 11, 10, 0),
                LocalDateTime.of(2026, 1, 11, 12, 0));

        // add
        mockMvc.perform(post("/api/v1/flights/" + f.getId() + "/departures/" + dep.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // remove
        mockMvc.perform(delete("/api/v1/flights/" + f.getId() + "/departures/" + dep.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}