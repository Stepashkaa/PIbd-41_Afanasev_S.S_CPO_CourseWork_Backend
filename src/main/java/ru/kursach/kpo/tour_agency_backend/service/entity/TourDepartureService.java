package ru.kursach.kpo.tour_agency_backend.service.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.mapper.TourDepartureMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.FlightRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourDepartureRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TourDepartureService {

    private final TourDepartureRepository tourDepartureRepository;
    private final TourRepository tourRepository;
    private final FlightRepository flightRepository;
    private final TourDepartureMapper tourDepartureMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponseDto<TourDepartureResponseDto> getMyPaged(
            Long userId,
            Long tourId,
            TourDepartureStatus status,
            LocalDate startFrom,
            LocalDate startTo,
            int page,
            int size
    ) {
        UserEntity manager = resolveUser(userId);

        if (manager.getUserRole() != UserRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ только для менеджеров");
        }

        Page<TourDepartureEntity> pageData =
                tourDepartureRepository.searchMyPaged(
                        manager.getId(),
                        tourId,
                        status,
                        startFrom,
                        startTo,
                        PageRequest.of(page, size, Sort.by("startDate").ascending())
                );

        return PageResponseDto.<TourDepartureResponseDto>builder()
                .page(pageData.getNumber())
                .size(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .content(pageData.getContent().stream()
                        .map(tourDepartureMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TourDepartureResponseDto> getAllPaged(
            Long tourId,
            TourDepartureStatus status,
            LocalDate startFrom,
            LocalDate startTo,
            int page,
            int size
    ) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("startDate").ascending().and(Sort.by("id").ascending())
        );

        if (startFrom != null && startTo != null && startFrom.isAfter(startTo)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "startFrom не может быть позже startTo"
            );
        }

        Page<TourDepartureEntity> departuresPage = tourDepartureRepository.searchPaged(
                tourId,
                status,
                startFrom,
                startTo,
                pageable
        );

        return PageResponseDto.<TourDepartureResponseDto>builder()
                .page(departuresPage.getNumber())
                .size(departuresPage.getSize())
                .totalPages(departuresPage.getTotalPages())
                .totalElements(departuresPage.getTotalElements())
                .content(departuresPage.getContent().stream()
                        .map(tourDepartureMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TourDepartureResponseDto> searchByTourId(
            Long tourId,
            int page,
            int size
    ) {
        if (tourId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр tourId обязателен для поиска вылетов по туру"
            );
        }

        return getAllPaged(tourId, null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TourDepartureResponseDto> getForFlight(
            Long flightId,
            TourDepartureStatus status,
            LocalDate startFrom,
            LocalDate startTo,
            int page,
            int size
    ) {
        FlightEntity flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + flightId + " не найден"
                ));

        Long depCityId = flight.getDepartureAirport().getCity().getId();
        Long arrCityId = flight.getArrivalAirport().getCity().getId();

        List<Long> cityIds = depCityId.equals(arrCityId)
                ? List.of(depCityId)
                : List.of(depCityId, arrCityId);

        // если даты не переданы с фронта — используем даты рейса
        LocalDate flightFrom = flight.getDepartAt().toLocalDate();
        LocalDate flightTo   = flight.getArriveAt().toLocalDate();

        LocalDate effectiveFrom = (startFrom != null) ? startFrom : flightFrom;
        LocalDate effectiveTo   = (startTo   != null) ? startTo   : flightTo;

        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startFrom не может быть позже startTo");
        }

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("startDate").ascending().and(Sort.by("id").ascending())
        );

        Page<TourDepartureEntity> departuresPage = tourDepartureRepository.searchByBaseCities(
                cityIds,
                status,
                effectiveFrom,
                effectiveTo,
                pageable
        );

        return PageResponseDto.<TourDepartureResponseDto>builder()
                .page(departuresPage.getNumber())
                .size(departuresPage.getSize())
                .totalPages(departuresPage.getTotalPages())
                .totalElements(departuresPage.getTotalElements())
                .content(departuresPage.getContent().stream()
                        .map(tourDepartureMapper::toDto)
                        .toList())
                .build();
    }


    @Transactional
    public TourDepartureResponseDto create(Long userId, TourDepartureCreateRequest request) {

        // ✅ твои проверки (оставляем)
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Дата окончания не может быть раньше даты начала"
            );
        }

        validateCapacity(request.capacityTotal(), request.capacityReserved());

        TourEntity tour = tourRepository.findById(request.tourId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Тур с id=" + request.tourId() + " не найден"
                ));

        validatePriceOverride(request.priceOverride(), tour);

        // ✅ ДОБАВЛЯЕМ: менеджер может создавать вылет только для своего тура
        UserEntity user = resolveUser(userId);
        if (user.getUserRole() == UserRole.MANAGER) {
            assertManagerOwnsTour(user, tour);
        }

        TourDepartureEntity departure = tourDepartureMapper.toEntity(request, tour);

        tour.addDeparture(departure);

        // ✅ твоя логика привязки flightIds (оставляем)
        if (request.flightIds() != null && !request.flightIds().isEmpty()) {
            List<FlightEntity> flights = flightRepository.findAllById(request.flightIds());

            if (flights.size() != request.flightIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Некоторые flightIds не существуют");
            }

            for (FlightEntity flight : flights) {
                validateFlightForDeparture(flight, departure);
                departure.addFlight(flight);
            }
        }

        departure = tourDepartureRepository.save(departure);
        return tourDepartureMapper.toDto(departure);
    }

    @Transactional(readOnly = true)
    public TourDepartureResponseDto getById(Long id) {
        TourDepartureEntity departure = tourDepartureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + id + " не найден"
                ));
        return tourDepartureMapper.toDto(departure);
    }

    @Transactional(readOnly = true)
    public List<TourDepartureResponseDto> getAll() {
        return tourDepartureRepository.findAll().stream()
                .map(tourDepartureMapper::toDto)
                .toList();
    }

    @Transactional
    public TourDepartureResponseDto update(Long userId, Long id, TourDepartureUpdateRequest request) {

        TourDepartureEntity departure = tourDepartureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + id + " не найден"
                ));

        // ✅ ДОБАВЛЯЕМ: менеджер может редактировать только свой вылет
        UserEntity user = resolveUser(userId);
        if (user.getUserRole() == UserRole.MANAGER) {
            assertManagerOwnsDeparture(user, departure);
        }

        // ✅ твои проверки (оставляем)
        validateCapacity(request.capacityTotal(), request.capacityReserved());

        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Дата окончания не может быть раньше даты начала");
        }

        TourEntity newTour = tourRepository.findById(request.tourId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Тур с id=" + request.tourId() + " не найден"
                ));

        validatePriceOverride(request.priceOverride(), newTour);

        // ✅ ДОБАВЛЯЕМ: если менеджер меняет tourId — новый тур тоже должен быть его
        if (user.getUserRole() == UserRole.MANAGER) {
            assertManagerOwnsTour(user, newTour);
        }

        // ✅ твоя логика переноса между турами (оставляем)
        TourEntity oldTour = departure.getTour();
        if (!oldTour.getId().equals(newTour.getId())) {
            oldTour.removeDeparture(departure);
            newTour.addDeparture(departure);
        }

        // ✅ твоя логика обновления flights (оставляем)
        if (request.flightIds() != null) {

            Set<Long> newIds = new HashSet<>(request.flightIds());

            for (FlightEntity oldFlight : List.copyOf(departure.getFlights())) {
                if (!newIds.contains(oldFlight.getId())) {
                    departure.removeFlight(oldFlight);
                }
            }

            List<FlightEntity> newFlights = flightRepository.findAllById(newIds);
            if (newFlights.size() != newIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Некоторые flightIds не существуют");
            }

            for (FlightEntity f : newFlights) {
                if (!departure.getFlights().contains(f)) {
                    validateFlightForDeparture(f, departure);
                    departure.addFlight(f);
                }
            }
        }

        tourDepartureMapper.updateEntity(request, newTour, departure);

        validateAllFlightsForDeparture(departure);

        departure = tourDepartureRepository.save(departure);
        return tourDepartureMapper.toDto(departure);
    }

    @Transactional
    public void delete(Long userId, Long id) {

        TourDepartureEntity departure = tourDepartureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + id + " не найден"
                ));

        // ✅ ДОБАВЛЯЕМ: менеджер может удалить только свой вылет
        UserEntity user = resolveUser(userId);
        if (user.getUserRole() == UserRole.MANAGER) {
            assertManagerOwnsDeparture(user, departure);
        }

        // ✅ твоя логика отвязки (оставляем)
        if (departure.getTour() != null) {
            departure.getTour().removeDeparture(departure);
        }

        for (FlightEntity flight : List.copyOf(departure.getFlights())) {
            departure.removeFlight(flight);
        }

        try {
            tourDepartureRepository.delete(departure);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить вылет тура: к нему привязаны бронирования"
            );
        }
    }

    private void validateCapacity(Integer capacityTotal, Integer capacityReserved) {
        int reserved = capacityReserved != null ? capacityReserved : 0;
        if (capacityTotal == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Вместимость (capacityTotal) обязательна"
            );
        }
        if (reserved > capacityTotal) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Забронированных мест не может быть больше вместимости"
            );
        }
    }

    private void validatePriceOverride(BigDecimal priceOverride, TourEntity tour) {
        if (priceOverride == null) {
            return;
        }
        // защита от отрицательных/нулевых — уже есть в DTO через @DecimalMin
        // здесь только логика "скидки"
        if (priceOverride.compareTo(tour.getBasePrice()) >= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Переопределённая цена должна быть меньше базовой цены тура (" +
                            tour.getBasePrice() + ")"
            );
        }
    }

    /**
     * Проверка, что рейс логически подходит к вылету тура:
     * 1) связан с базовым городом тура;
     * 2) даты рейса пересекаются с датами вылета тура.
     */
    private void validateFlightForDeparture(FlightEntity flight, TourDepartureEntity departure) {
        Long baseCityId = departure.getTour().getBaseCity().getId();
        Long depCityId = flight.getDepartureAirport().getCity().getId();
        Long arrCityId = flight.getArrivalAirport().getCity().getId();

        if (!depCityId.equals(baseCityId) && !arrCityId.equals(baseCityId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Рейс " + flight.getFlightNumber() +
                            " не связан с базовым городом тура " +
                            departure.getTour().getBaseCity().getName()
            );
        }

        LocalDate depStart = departure.getStartDate();
        LocalDate depEnd = departure.getEndDate();
        LocalDate flightDepartDate = flight.getDepartAt().toLocalDate();
        LocalDate flightArriveDate = flight.getArriveAt().toLocalDate();

        // Проверка пересечения интервалов [flightDepartDate, flightArriveDate] и [depStart, depEnd]
        if (flightArriveDate.isBefore(depStart) || flightDepartDate.isAfter(depEnd)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Даты рейса " + flight.getFlightNumber() +
                            " не пересекаются с диапазоном дат вылета тура (" +
                            depStart + " - " + depEnd + ")"
            );
        }
    }

    private void validateAllFlightsForDeparture(TourDepartureEntity departure) {
        for (FlightEntity flight : departure.getFlights()) {
            validateFlightForDeparture(flight, departure);
        }
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Пользователь с id=" + userId + " не найден"
                ));
    }

    private void assertManagerOwnsTour(UserEntity manager, TourEntity tour) {
        if (tour.getManagerUser() == null ||
                !tour.getManagerUser().getId().equals(manager.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Нет доступа к туру другого менеджера"
            );
        }
    }

    private void assertManagerOwnsDeparture(UserEntity manager, TourDepartureEntity departure) {
        assertManagerOwnsTour(manager, departure.getTour());
    }
}
