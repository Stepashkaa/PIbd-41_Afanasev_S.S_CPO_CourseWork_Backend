package ru.kursach.kpo.tour_agency_backend.service.entity;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.mapper.FlightMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.repository.AirportRepository;
import ru.kursach.kpo.tour_agency_backend.repository.FlightRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourDepartureRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final TourRepository tourRepository;
    private final FlightMapper flightMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<FlightResponseDto> getFlightsForDeparture(
            Long departureId,
            String flightNumberFilter,
            int page,
            int size
    ) {
        TourDepartureEntity departure = tourDepartureRepository.findById(departureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + departureId + " не найден"
                ));

        TourEntity tour = departure.getTour();
        Long cityId = tour.getBaseCity().getId();

        String flightNumber = (flightNumberFilter != null && !flightNumberFilter.isBlank())
                ? flightNumberFilter.trim()
                : null;

        PageRequest pageable = PageRequest.of(page, size, Sort.by("flightNumber").ascending());

        Page<FlightEntity> flights = flightRepository.searchByCityAndNumber(
                cityId,
                flightNumber,
                pageable
        );

        LocalDate depStart = departure.getStartDate();
        LocalDate depEnd = departure.getEndDate();

        List<FlightResponseDto> content = flights.getContent().stream()
                .filter(f -> {
                    LocalDate fd = f.getDepartAt().toLocalDate();
                    LocalDate fa = f.getArriveAt().toLocalDate();
                    return !(fa.isBefore(depStart) || fd.isAfter(depEnd)); // пересечение интервалов
                })
                .map(flightMapper::toDto)
                .toList();

        return PageResponseDto.<FlightResponseDto>builder()
                .page(flights.getNumber())
                .size(flights.getSize())
                .totalPages(flights.getTotalPages())
                .totalElements(flights.getTotalElements())
                .content(content)
                .build();
    }


    @Transactional(readOnly = true)
    public PageResponseDto<FlightResponseDto> getFlightsForTourBaseCity(
            Long tourId,
            String flightNumberFilter,
            int page,
            int size
    ) {
        TourEntity tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тур с id=" + tourId + " не найден"
                ));

        Long cityId = tour.getBaseCity().getId();

        String flightNumber = (flightNumberFilter != null && !flightNumberFilter.isBlank())
                ? flightNumberFilter.trim()
                : null;

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("flightNumber").ascending()
        );

        Page<FlightEntity> flights = flightRepository.searchByCityAndNumber(
                cityId,
                flightNumber,
                pageable
        );

        return PageResponseDto.<FlightResponseDto>builder()
                .page(flights.getNumber())
                .size(flights.getSize())
                .totalPages(flights.getTotalPages())
                .totalElements(flights.getTotalElements())
                .content(flights.getContent().stream()
                        .map(flightMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<FlightResponseDto> getAllPaged(
            String flightNumberFilter,
            String departureAirportNameFilter,
            String arrivalAirportNameFilter,
            int page,
            int size
    ) {
        String flightNumber = (flightNumberFilter != null && !flightNumberFilter.trim().isEmpty())
                ? flightNumberFilter.trim().toLowerCase()
                : "";

        String departureAirportName = (departureAirportNameFilter != null && !departureAirportNameFilter.trim().isEmpty())
                ? departureAirportNameFilter.trim().toLowerCase()
                : "";

        String arrivalAirportName = (arrivalAirportNameFilter != null && !arrivalAirportNameFilter.trim().isEmpty())
                ? arrivalAirportNameFilter.trim().toLowerCase()
                : "";

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("flightNumber").ascending()
        );

        Page<FlightEntity> flightPage = flightRepository.search(
                flightNumber,
                departureAirportName,
                arrivalAirportName,
                pageable
        );

        return PageResponseDto.<FlightResponseDto>builder()
                .page(flightPage.getNumber())
                .size(flightPage.getSize())
                .totalPages(flightPage.getTotalPages())
                .totalElements(flightPage.getTotalElements())
                .content(flightPage.getContent().stream()
                        .map(flightMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public FlightResponseDto findByFlightNumber(String number) {
        if (number == null || number.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Номер рейса обязателен");

        FlightEntity flight = flightRepository.findByFlightNumberIgnoreCase(number.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с номером '" + number + "' не найден"
                ));

        return flightMapper.toDto(flight);
    }

    @Transactional
    public FlightResponseDto create(FlightCreateRequest request) {
        if (flightRepository.existsByFlightNumberIgnoreCase(request.flightNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Рейс с таким номером уже существует"
            );
        }

        if (request.arriveAt().isBefore(request.departAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Время прилёта не может быть раньше времени вылета"
            );
        }

        AirportEntity departureAirport = airportRepository.findById(request.departureAirportId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Аэропорт вылета с id=" + request.departureAirportId() + " не найден"
                ));

        AirportEntity arrivalAirport = airportRepository.findById(request.arrivalAirportId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Аэропорт прилёта с id=" + request.arrivalAirportId() + " не найден"
                ));

        validateAirportsNotSame(departureAirport, arrivalAirport);

        FlightEntity flight = flightMapper.toEntity(request, departureAirport, arrivalAirport);

        departureAirport.addDepartureFlight(flight);
        arrivalAirport.addArrivalFlight(flight);

        flight = flightRepository.save(flight);
        return flightMapper.toDto(flight);
    }

    @Transactional(readOnly = true)
    public FlightResponseDto getById(Long id) {
        FlightEntity flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + id + " не найден"
                ));
        return flightMapper.toDto(flight);
    }

    @Transactional(readOnly = true)
    public List<FlightResponseDto> getAll() {
        return flightRepository.findAll().stream()
                .map(flightMapper::toDto)
                .toList();
    }

    @Transactional
    public FlightResponseDto update(Long id, FlightUpdateRequest request) {
        FlightEntity flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + id + " не найден"
                ));

        boolean existsNumber = flightRepository.existsByFlightNumberIgnoreCase(request.flightNumber());
        if (existsNumber && !flight.getFlightNumber().equalsIgnoreCase(request.flightNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Рейс с таким номером уже существует"
            );
        }

        if (request.arriveAt().isBefore(request.departAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Время прилёта не может быть раньше времени вылета"
            );
        }

        AirportEntity newDeparture = airportRepository.findById(request.departureAirportId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Аэропорт вылета с id=" + request.departureAirportId() + " не найден"
                ));

        AirportEntity newArrival = airportRepository.findById(request.arrivalAirportId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Аэропорт прилёта с id=" + request.arrivalAirportId() + " не найден"
                ));

        validateAirportsNotSame(newDeparture, newArrival);

        AirportEntity oldDeparture = flight.getDepartureAirport();
        AirportEntity oldArrival = flight.getArrivalAirport();

        if (!oldDeparture.getId().equals(newDeparture.getId())) {
            oldDeparture.removeDepartureFlight(flight);
            newDeparture.addDepartureFlight(flight);
        }

        if (!oldArrival.getId().equals(newArrival.getId())) {
            oldArrival.removeArrivalFlight(flight);
            newArrival.addArrivalFlight(flight);
        }

        flightMapper.updateEntity(request, flight);

        validateAllDeparturesForFlight(flight);
        flight = flightRepository.save(flight);

        return flightMapper.toDto(flight);
    }

    @Transactional
    public void delete(Long id) {
        FlightEntity flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + id + " не найден"
                ));

        AirportEntity departure = flight.getDepartureAirport();
        AirportEntity arrival = flight.getArrivalAirport();

        if (departure != null) {
            departure.removeDepartureFlight(flight);
        }
        if (arrival != null) {
            arrival.removeArrivalFlight(flight);
        }

        List<TourDepartureEntity> tourDeps = List.copyOf(flight.getTourDepartures());
        tourDeps.forEach(flight::removeTourDeparture);

        try {
            flightRepository.delete(flight);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить рейс: на него ссылаются туры или бронирования"
            );
        }
    }

    public FlightResponseDto addDepartureToFlight(Long flightId, Long departureId) {

        FlightEntity flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + flightId + " не найден"
                ));

        TourDepartureEntity departure = tourDepartureRepository.findById(departureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + departureId + " не найден"
                ));

        if (flight.getTourDepartures().contains(departure)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Этот вылет уже привязан к рейсу"
            );
        }

        validateDepartureForFlight(departure, flight);

        flight.addTourDeparture(departure);

        flight = flightRepository.save(flight);
        return flightMapper.toDto(flight);
    }

    public FlightResponseDto removeDepartureFromFlight(Long flightId, Long departureId) {

        FlightEntity flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Рейс с id=" + flightId + " не найден"
                ));

        TourDepartureEntity departure = tourDepartureRepository.findById(departureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + departureId + " не найден"
                ));

        if (!flight.getTourDepartures().contains(departure)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Этот вылет не привязан к рейсу"
            );
        }

        flight.removeTourDeparture(departure);

        flight = flightRepository.save(flight);
        return flightMapper.toDto(flight);
    }

    private void validateAirportsNotSame(AirportEntity departure, AirportEntity arrival) {
        if (departure.getId().equals(arrival.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Аэропорт вылета и прилёта не могут совпадать для одного рейса"
            );
        }
    }

    /**
     * Зеркальная логика для проверки вылета тура относительно рейса:
     * 1) базовый город тура = город вылета или прилёта рейса;
     * 2) даты рейса пересекаются с датами вылета тура.
     */
    private void validateDepartureForFlight(TourDepartureEntity departure, FlightEntity flight) {
        Long baseCityId = departure.getTour().getBaseCity().getId();
        Long depCityId = flight.getDepartureAirport().getCity().getId();
        Long arrCityId = flight.getArrivalAirport().getCity().getId();

        if (!depCityId.equals(baseCityId) && !arrCityId.equals(baseCityId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Вылет тура с id=" + departure.getId() +
                            " относится к базовому городу " +
                            departure.getTour().getBaseCity().getName() +
                            ", который не совпадает с городами рейса " +
                            flight.getFlightNumber()
            );
        }

        LocalDate depStart = departure.getStartDate();
        LocalDate depEnd = departure.getEndDate();
        LocalDate flightDepartDate = flight.getDepartAt().toLocalDate();
        LocalDate flightArriveDate = flight.getArriveAt().toLocalDate();

        if (flightArriveDate.isBefore(depStart) || flightDepartDate.isAfter(depEnd)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Даты рейса " + flight.getFlightNumber() +
                            " не пересекаются с диапазоном дат вылета тура (" +
                            depStart + " - " + depEnd + ")"
            );
        }
    }
    /**
     * Повторно валидирует все вылеты тура, уже привязанные к рейсу.
     * Нужен на случай, когда у рейса меняются аэропорты или даты.
     */
    private void validateAllDeparturesForFlight(FlightEntity flight) {
        for (TourDepartureEntity departure : flight.getTourDepartures()) {
            validateDepartureForFlight(departure, flight);
        }
    }

}