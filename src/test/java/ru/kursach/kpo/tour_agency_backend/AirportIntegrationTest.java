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
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.AirportRepository;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AirportIntegrationTest {

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

    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
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

    // ---------------- tests ----------------

    @Test
    @DisplayName("GET /api/v1/airports без токена -> 403 (ADMIN only)")
    void getAll_noToken_403() throws Exception {
        mockMvc.perform(get("/api/v1/airports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/airports с USER токеном -> 403 (ADMIN only)")
    void getAll_userToken_403() throws Exception {
        String userToken = signUpUserAndGetToken("user1@gmail.com");

        mockMvc.perform(get("/api/v1/airports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/airports с ADMIN -> 201, затем GET by id -> 200")
    void create_admin_201_and_getById_200() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity city = createCity("Riga", "Latvia");

        String createBody = """
          {"iataCode":"RIX","name":"Riga International Airport","cityId":%d}
        """.formatted(city.getId());

        String createdJson = mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.iataCode").value("RIX"))
                .andExpect(jsonPath("$.cityId").value(city.getId()))
                .andReturn().getResponse().getContentAsString();

        long airportId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(get("/api/v1/airports/" + airportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(airportId))
                .andExpect(jsonPath("$.cityName").value("Riga"));
    }

    @Test
    @DisplayName("POST /api/v1/airports с ADMIN, но cityId не существует -> 400")
    void create_admin_badCity_400() throws Exception {
        String adminToken = createAdminAndGetToken();

        String createBody = """
          {"iataCode":"RIX","name":"Riga International Airport","cityId":999999}
        """;

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/airports дубль iataCode -> 400")
    void create_duplicateIata_400() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity city = createCity("Riga", "Latvia");

        String body1 = """
          {"iataCode":"RIX","name":"Riga Airport","cityId":%d}
        """.formatted(city.getId());

        String body2 = """
          {"iataCode":"RIX","name":"Another name","cityId":%d}
        """.formatted(city.getId());

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/airports/search/by-iata с ADMIN -> 200")
    void searchByIata_admin_200() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity city = createCity("Riga", "Latvia");

        String createBody = """
          {"iataCode":"RIX","name":"Riga Airport","cityId":%d}
        """.formatted(city.getId());

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/airports/search/by-iata")
                        .param("iata", "RIX")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iataCode").value("RIX"))
                .andExpect(jsonPath("$.cityName").value("Riga"));
    }

    @Test
    @DisplayName("PUT /api/v1/airports/{id} с ADMIN -> 200")
    void update_admin_200() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity city = createCity("Riga", "Latvia");

        String createBody = """
          {"iataCode":"RIX","name":"Riga Airport","cityId":%d}
        """.formatted(city.getId());

        String createdJson = mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long airportId = objectMapper.readTree(createdJson).get("id").asLong();

        String updateBody = """
          {"iataCode":"RIX","name":"Riga Airport Updated","cityId":%d}
        """.formatted(city.getId());

        mockMvc.perform(put("/api/v1/airports/" + airportId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(airportId))
                .andExpect(jsonPath("$.name").value("Riga Airport Updated"));
    }

    @Test
    @DisplayName("GET /api/v1/airports/paged с ADMIN -> 200 и фильтрация по cityId")
    void paged_admin_200_filterByCity() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity riga = createCity("Riga", "Latvia");
        CityEntity vilnius = createCity("Vilnius", "Lithuania");

        String a1 = """
          {"iataCode":"RIX","name":"Riga Airport","cityId":%d}
        """.formatted(riga.getId());

        String a2 = """
          {"iataCode":"VNO","name":"Vilnius Airport","cityId":%d}
        """.formatted(vilnius.getId());

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(a1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(a2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/airports/paged")
                        .param("cityId", String.valueOf(riga.getId()))
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].iataCode").value("RIX"));
    }

    @Test
    @DisplayName("DELETE /api/v1/airports/{id} с ADMIN -> 204")
    void delete_admin_204() throws Exception {
        String adminToken = createAdminAndGetToken();
        CityEntity city = createCity("Riga", "Latvia");

        String createBody = """
          {"iataCode":"RIX","name":"Riga Airport","cityId":%d}
        """.formatted(city.getId());

        String createdJson = mockMvc.perform(post("/api/v1/airports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long airportId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(delete("/api/v1/airports/" + airportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/airports/" + airportId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}