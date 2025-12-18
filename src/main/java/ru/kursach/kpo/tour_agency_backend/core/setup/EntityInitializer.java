package ru.kursach.kpo.tour_agency_backend.core.setup;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.model.entity.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.*;
import ru.kursach.kpo.tour_agency_backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class EntityInitializer {

    private static final Logger logger = LoggerFactory.getLogger(EntityInitializer.class);

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;
    private final TourRepository tourRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final FlightRepository flightRepository;
    private final AtomicInteger flightSeq = new AtomicInteger(1000);

    private final Random random = new Random();

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.data.recreate:false}")
    private boolean recreateData;

    private static final List<String> EUROPEAN_CITIES = List.of(
            "Париж", "Рим", "Барселона", "Лондон", "Амстердам",
            "Прага", "Вена", "Будапешт", "Берлин", "Мюнхен",
            "Венеция", "Флоренция", "Афины", "Стамбул", "Дубровник"
    );

    private static final List<String> TOUR_TITLES = List.of(
            "Романтический тур по Европе", "Гастрономическое путешествие", "Исторические столицы",
            "Пляжный отдых", "Горнолыжные каникулы", "Экскурсионный тур",
            "Авторский маршрут", "Премиум путешествие", "Эконом тур",
            "Семейный отдых", "Молодежное приключение", "Люкс путешествие",
            "Культурный тур", "Шоппинг тур", "Винный тур"
    );

    private static final List<String> TOUR_DESCRIPTIONS = List.of(
            "Незабываемое путешествие по лучшим городам Европы",
            "Идеальный тур для любителей истории и культуры",
            "Комфортабельный отдых с насыщенной экскурсионной программой",
            "Тур для настоящих гурманов и ценителей хорошей кухни",
            "Активный отдых и приключения в самых живописных местах",
            "Роскошный тур с проживанием в лучших отелях"
    );

    private static final List<String> CARRIERS = List.of(
            "Аэрофлот", "S7 Airlines", "Turkish Airlines", "Emirates",
            "Lufthansa", "Air France", "British Airways", "Finnair"
    );

    @PostConstruct
    @Transactional
    public void initializeAll() {
        logger.info("=== DB INITIALIZATION START ===");

        UserEntity admin = createAdminIfNeeded();
        if (admin == null) return;

        if (cityRepository.count() > 0 && !recreateData) {
            logger.info("Demo data already exists, skipping initialization.");
            return;
        }

        if (recreateData) {
            logger.info("Recreating all demo data...");
            clearAllData();
        }

        List<CityEntity> cities = createCities();
        List<AirportEntity> airports = createAirports(cities);
        List<UserEntity> managers = createManagers(admin);
        List<TourEntity> tours = createRandomTours(cities, managers);
        List<TourDepartureEntity> departures = createRandomDepartures(tours);
        createRandomFlights(departures, airports);
        createUsers();

        logger.info("=== DB INITIALIZATION FINISHED ===");
        logger.info("Created: {} cities, {} airports, {} tours, {} departures",
                cities.size(), airports.size(), tours.size(), departures.size());
    }

    private void clearAllData() {
        flightRepository.deleteAllInBatch();
        tourDepartureRepository.deleteAllInBatch();
        tourRepository.deleteAllInBatch();
        airportRepository.deleteAllInBatch();
        cityRepository.deleteAllInBatch();
    }

    /* ===================== USERS ===================== */

    private UserEntity createAdminIfNeeded() {
        return userRepository.findByEmail(adminEmail).orElseGet(() -> {
            logger.info("Creating admin user: {}", adminEmail);

            UserEntity admin = UserEntity.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .phone("+79991234567")
                    .userRole(UserRole.ADMIN)
                    .active(true)
                    .build();

            return userRepository.save(admin);
        });
    }

    private List<UserEntity> createManagers(UserEntity admin) {
        List<UserEntity> managers = new ArrayList<>();
        managers.add(admin); // Админ тоже менеджер

        List<String> managerEmails = List.of(
                "manager1@agency.com",
                "manager2@agency.com",
                "manager3@agency.com"
        );

        for (int i = 0; i < managerEmails.size(); i++) {
            UserEntity manager = UserEntity.builder()
                    .username("manager" + (i + 1))
                    .email(managerEmails.get(i))
                    .password(passwordEncoder.encode("Manager123!"))
                    .phone("+7999" + (1000000 + random.nextInt(9000000)))
                    .userRole(UserRole.MANAGER)
                    .active(true)
                    .build();
            managers.add(userRepository.save(manager));
        }

        return managers;
    }

    private void createUsers() {
        for (int i = 1; i <= 20; i++) {
            UserEntity user = UserEntity.builder()
                    .username("client" + i)
                    .email("client" + i + "@example.com")
                    .password(passwordEncoder.encode("Client123!"))
                    .phone("+7999" + (1000000 + random.nextInt(9000000)))
                    .userRole(UserRole.USER)
                    .active(random.nextDouble() > 0.1) // 90% активны
                    .build();
            userRepository.save(user);
        }
    }

    /* ===================== CITIES ===================== */

    private List<CityEntity> createCities() {
        List<CityEntity> cities = new ArrayList<>();

        // Российские города
        cities.add(createCity("Москва", "Россия", "Europe/Moscow"));
        cities.add(createCity("Санкт-Петербург", "Россия", "Europe/Moscow"));
        cities.add(createCity("Сочи", "Россия", "Europe/Moscow"));
        cities.add(createCity("Казань", "Россия", "Europe/Moscow"));

        // Европейские города
        for (String cityName : EUROPEAN_CITIES) {
            String country = getCountryByCity(cityName);
            String timezone = getTimezoneByCity(cityName);
            cities.add(createCity(cityName, country, timezone));
        }

        // Азиатские направления
        cities.add(createCity("Бангкок", "Тайланд", "Asia/Bangkok"));
        cities.add(createCity("Дубай", "ОАЭ", "Asia/Dubai"));
        cities.add(createCity("Токио", "Япония", "Asia/Tokyo"));

        return cityRepository.saveAll(cities);
    }

    private CityEntity createCity(String name, String country, String timezone) {
        return CityEntity.builder()
                .name(name)
                .country(country)
                .timezone(timezone)
                .build();
    }

    private String getCountryByCity(String city) {
        Map<String, String> cityToCountry = Map.ofEntries(
                Map.entry("Париж", "Франция"),
                Map.entry("Рим", "Италия"),
                Map.entry("Барселона", "Испания"),
                Map.entry("Лондон", "Великобритания"),
                Map.entry("Амстердам", "Нидерланды"),
                Map.entry("Прага", "Чехия"),
                Map.entry("Вена", "Австрия"),
                Map.entry("Будапешт", "Венгрия"),
                Map.entry("Берлин", "Германия"),
                Map.entry("Мюнхен", "Германия"),
                Map.entry("Венеция", "Италия"),
                Map.entry("Флоренция", "Италия"),
                Map.entry("Афины", "Греция"),
                Map.entry("Стамбул", "Турция"),
                Map.entry("Дубровник", "Хорватия")
        );
        return cityToCountry.getOrDefault(city, city + " Страна");
    }

    private String getTimezoneByCity(String city) {
        Map<String, String> cityToTimezone = Map.ofEntries(
                Map.entry("Москва", "Europe/Moscow"),
                Map.entry("Париж", "Europe/Paris"),
                Map.entry("Рим", "Europe/Rome"),
                Map.entry("Барселона", "Europe/Madrid"),
                Map.entry("Лондон", "Europe/London"),
                Map.entry("Амстердам", "Europe/Amsterdam"),
                Map.entry("Берлин", "Europe/Berlin"),
                Map.entry("Афины", "Europe/Athens"),
                Map.entry("Стамбул", "Europe/Istanbul"),
                Map.entry("Бангкок", "Asia/Bangkok"),
                Map.entry("Дубай", "Asia/Dubai"),
                Map.entry("Токио", "Asia/Tokyo")
        );
        return cityToTimezone.getOrDefault(city, "UTC");
    }

    /* ===================== AIRPORTS ===================== */

    private List<AirportEntity> createAirports(List<CityEntity> cities) {
        Map<String, AirportInfo> airportMap = new HashMap<>();

        // Российские аэропорты
        airportMap.put("Москва", new AirportInfo("Шереметьево", "SVO"));
        airportMap.put("Санкт-Петербург", new AirportInfo("Пулково", "LED"));
        airportMap.put("Сочи", new AirportInfo("Адлер", "AER"));
        airportMap.put("Казань", new AirportInfo("Казань", "KZN"));

        // Международные аэропорты
        airportMap.put("Париж", new AirportInfo("Шарль де Голль", "CDG"));
        airportMap.put("Рим", new AirportInfo("Фьюмичино", "FCO"));
        airportMap.put("Барселона", new AirportInfo("Барселона-Эль Прат", "BCN"));
        airportMap.put("Лондон", new AirportInfo("Хитроу", "LHR"));
        airportMap.put("Амстердам", new AirportInfo("Схипхол", "AMS"));
        airportMap.put("Прага", new AirportInfo("Прага", "PRG"));
        airportMap.put("Вена", new AirportInfo("Вена", "VIE"));
        airportMap.put("Будапешт", new AirportInfo("Ференц Лист", "BUD"));
        airportMap.put("Берлин", new AirportInfo("Бранденбург", "BER"));
        airportMap.put("Мюнхен", new AirportInfo("Мюнхен", "MUC"));
        airportMap.put("Венеция", new AirportInfo("Марко Поло", "VCE"));
        airportMap.put("Флоренция", new AirportInfo("Флоренция", "FLR"));
        airportMap.put("Афины", new AirportInfo("Афины", "ATH"));
        airportMap.put("Стамбул", new AirportInfo("Стамбул", "IST"));
        airportMap.put("Дубровник", new AirportInfo("Дубровник", "DBV"));
        airportMap.put("Бангкок", new AirportInfo("Суварнабхуми", "BKK"));
        airportMap.put("Дубай", new AirportInfo("Дубай", "DXB"));
        airportMap.put("Токио", new AirportInfo("Нарита", "NRT"));

        List<AirportEntity> airports = new ArrayList<>();

        for (CityEntity city : cities) {
            AirportInfo info = airportMap.get(city.getName());
            if (info != null) {
                airports.add(AirportEntity.builder()
                        .iataCode(info.iataCode)
                        .name(info.name)
                        .city(city)
                        .build());
            } else {
                // Если нет в мапе, создаем по умолчанию
                airports.add(AirportEntity.builder()
                        .iataCode(generateIataCode(city))
                        .name("Аэропорт " + city.getName())
                        .city(city)
                        .build());
            }
        }

        return airportRepository.saveAll(airports);
    }

    private String generateIataCode(CityEntity city) {
        String name = city.getName();
        if (name.length() >= 3) {
            return name.substring(0, 3).toUpperCase();
        }
        return (name + "XXX").substring(0, 3).toUpperCase();
    }

    record AirportInfo(String name, String iataCode) {}

    /* ===================== TOURS ===================== */

    private List<TourEntity> createRandomTours(List<CityEntity> cities, List<UserEntity> managers) {
        int count = 50; // 50 различных туров
        List<TourEntity> tours = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CityEntity city = cities.get(random.nextInt(cities.size()));
            UserEntity manager = managers.get(random.nextInt(managers.size()));

            int duration = switch (random.nextInt(5)) {
                case 0 -> 3;  // Короткие (3 дня)
                case 1 -> 5;  // Средние (5 дней)
                case 2 -> 7;  // Стандартные (7 дней)
                case 3 -> 10; // Длинные (10 дней)
                default -> 14; // Очень длинные (14 дней)
            };

            BigDecimal basePrice = switch (duration) {
                case 3 -> BigDecimal.valueOf(20_000 + random.nextInt(30_000));
                case 5 -> BigDecimal.valueOf(40_000 + random.nextInt(50_000));
                case 7 -> BigDecimal.valueOf(60_000 + random.nextInt(80_000));
                case 10 -> BigDecimal.valueOf(100_000 + random.nextInt(100_000));
                default -> BigDecimal.valueOf(150_000 + random.nextInt(150_000));
            };

            // Для азиатских и роскошных туров цена выше
            if (city.getCountry().equals("ОАЭ") || city.getCountry().equals("Япония")) {
                basePrice = basePrice.multiply(BigDecimal.valueOf(1.5));
            }

            TourEntity tour = TourEntity.builder()
                    .title(TOUR_TITLES.get(random.nextInt(TOUR_TITLES.size())) + " в " + city.getName())
                    .description(TOUR_DESCRIPTIONS.get(random.nextInt(TOUR_DESCRIPTIONS.size())) +
                            ". Продолжительность: " + duration + " дней.")
                    .durationDays(duration)
                    .basePrice(basePrice)
                    .status(random.nextDouble() > 0.1 ? TourStatus.PUBLISHED : TourStatus.DRAFT)
                    .active(random.nextDouble() > 0.05) // 95% активны
                    .baseCity(city)
                    .managerUser(manager)
                    .build();

            tours.add(tour);
        }

        return tourRepository.saveAll(tours);
    }

    /* ===================== TOUR DEPARTURES ===================== */

    private List<TourDepartureEntity> createRandomDepartures(List<TourEntity> tours) {
        List<TourDepartureEntity> departures = new ArrayList<>();

        for (TourEntity tour : tours) {
            // Для каждого тура создаем 2-5 вылетов в разные даты
            int departureCount = 2 + random.nextInt(4);

            for (int i = 0; i < departureCount; i++) {
                // Даты в ближайшие 180 дней
                int daysOffset = 7 + random.nextInt(180);
                LocalDate startDate = LocalDate.now().plusDays(daysOffset);
                LocalDate endDate = startDate.plusDays(tour.getDurationDays());

                int capacityTotal = switch (tour.getBasePrice().compareTo(BigDecimal.valueOf(100_000))) {
                    case -1 -> 30 + random.nextInt(20); // Дешевые туры - больше мест
                    case 0 -> 20 + random.nextInt(15);  // Средние туры
                    default -> 10 + random.nextInt(10); // Дорогие туры - меньше мест
                };

                int capacityReserved = random.nextInt(Math.max(1, capacityTotal / 3));

                // Иногда делаем специальные цены
                BigDecimal priceOverride = null;
                if (random.nextDouble() > 0.7) {
                    double discount = 0.8 + random.nextDouble() * 0.3; // 80-110% от базовой
                    priceOverride = tour.getBasePrice().multiply(BigDecimal.valueOf(discount));
                }

                TourDepartureStatus status = TourDepartureStatus.PLANNED;

// Проверяем даты для определения статуса
                LocalDate now = LocalDate.now();

                if (startDate.isBefore(now)) {
                    // Вылет уже начался или прошел
                    if (endDate.isAfter(now)) {
                        status = TourDepartureStatus.IN_PROGRESS;  // Тур сейчас идет
                    } else {
                        status = TourDepartureStatus.COMPLETED;    // Тур завершен
                    }
                } else if (capacityReserved >= capacityTotal) {
                    status = TourDepartureStatus.SALES_CLOSED;     // Мест нет
                } else if (random.nextDouble() > 0.9) {
                    status = TourDepartureStatus.CANCELLED;        // 10% шанс отмены
                } else if (startDate.isBefore(now.plusDays(3))) {
                    // Скоро старт (в ближайшие 3 дня) - можно добавить CONFIRMED если нужен
                    // Но у вас нет такого статуса, оставляем PLANNED
                }

                departures.add(TourDepartureEntity.builder()
                        .tour(tour)
                        .startDate(startDate)
                        .endDate(endDate)
                        .capacityTotal(capacityTotal)
                        .capacityReserved(capacityReserved)
                        .priceOverride(priceOverride)
                        .status(status)
                        .build());
            }
        }

        return tourDepartureRepository.saveAll(departures);
    }

    /* ===================== FLIGHTS ===================== */

    private void createRandomFlights(List<TourDepartureEntity> departures, List<AirportEntity> airports) {
        // Основные хабы в России
        AirportEntity svo = findAirportByIata(airports, "SVO");
        AirportEntity led = findAirportByIata(airports, "LED");
        AirportEntity aer = findAirportByIata(airports, "AER");

        List<AirportEntity> russianHubs = List.of(svo, led, aer);

        Set<String> usedFlightNumbers = new HashSet<>();

        for (TourDepartureEntity departure : departures) {
            // Пропускаем отмененные вылеты
            if (departure.getStatus() == TourDepartureStatus.CANCELLED) {
                continue;
            }

            // Выбираем российский аэропорт отправления
            AirportEntity departureAirport = russianHubs.get(random.nextInt(russianHubs.size()));

            // Находим аэропорт назначения (город тура)
            CityEntity tourCity = departure.getTour().getBaseCity();
            AirportEntity arrivalAirport = airports.stream()
                    .filter(a -> a.getCity().equals(tourCity))
                    .findFirst()
                    .orElse(departureAirport);

            // Если это тот же город, выбираем другой
            if (arrivalAirport.equals(departureAirport)) {
                arrivalAirport = airports.stream()
                        .filter(a -> !a.equals(departureAirport))
                        .findFirst()
                        .orElse(departureAirport);
            }

            // Генерируем уникальный номер рейса
            String flightNumber;
            do {
                String carrierCode = CARRIERS.get(random.nextInt(CARRIERS.size()))
                        .substring(0, 2).toUpperCase();
                flightNumber = carrierCode + (1000 + random.nextInt(9000));
            } while (usedFlightNumbers.contains(flightNumber));
            usedFlightNumbers.add(flightNumber);

            // Время вылета
            LocalTime departTime = LocalTime.of(6 + random.nextInt(12), random.nextBoolean() ? 0 : 30);
            LocalDateTime departAt = departure.getStartDate().atTime(departTime);

            // Расчет времени полета (зависит от расстояния)
            int flightHours = 2 + random.nextInt(8);
            LocalDateTime arriveAt = departAt.plusHours(flightHours);

            // Цена билета зависит от направления и времени
            BigDecimal basePrice = BigDecimal.valueOf(5_000 + random.nextInt(25_000));
            if (flightHours > 6) {
                basePrice = basePrice.multiply(BigDecimal.valueOf(1.5));
            }

            // Создаем рейс туда
            FlightEntity outboundFlight = FlightEntity.builder()
                    .flightNumber(flightNumber)
                    .carrier(CARRIERS.get(random.nextInt(CARRIERS.size())))
                    .departAt(departAt)
                    .arriveAt(arriveAt)
                    .basePrice(basePrice)
                    .departureAirport(departureAirport)
                    .arrivalAirport(arrivalAirport)
                    .status(FlightStatus.SCHEDULED)
                    .build();

            outboundFlight.addTourDeparture(departure);
            flightRepository.save(outboundFlight);

            // Для длительных туров создаем обратный рейс
            if (departure.getTour().getDurationDays() > 5 && random.nextDouble() > 0.3) {
                String returnFlightNumber;
                do {
                    String carrierCode = CARRIERS.get(random.nextInt(CARRIERS.size()))
                            .substring(0, 2).toUpperCase();
                    returnFlightNumber = carrierCode + (1000 + random.nextInt(9000));
                } while (usedFlightNumbers.contains(returnFlightNumber));
                usedFlightNumbers.add(returnFlightNumber);

                LocalDateTime returnDepartAt = departure.getEndDate()
                        .atTime(departTime.plusHours(1 + random.nextInt(3)));
                LocalDateTime returnArriveAt = returnDepartAt.plusHours(flightHours);

                FlightEntity returnFlight = FlightEntity.builder()
                        .flightNumber(returnFlightNumber)
                        .carrier(CARRIERS.get(random.nextInt(CARRIERS.size())))
                        .departAt(returnDepartAt)
                        .arriveAt(returnArriveAt)
                        .basePrice(basePrice.multiply(BigDecimal.valueOf(0.9))) // Обратный дешевле
                        .departureAirport(arrivalAirport)
                        .arrivalAirport(departureAirport)
                        .status(FlightStatus.SCHEDULED)
                        .build();

                returnFlight.addTourDeparture(departure);
                flightRepository.save(returnFlight);
            }
        }
    }

    private AirportEntity findAirportByIata(List<AirportEntity> airports, String iata) {
        return airports.stream()
                .filter(a -> a.getIataCode().equals(iata))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Airport not found: " + iata));
    }
}