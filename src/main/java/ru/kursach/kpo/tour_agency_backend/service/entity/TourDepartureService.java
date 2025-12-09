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
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;
import ru.kursach.kpo.tour_agency_backend.repository.FlightRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourDepartureRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Transactional
    public TourDepartureResponseDto create(TourDepartureCreateRequest request) {

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

        TourDepartureEntity departure = tourDepartureMapper.toEntity(request, tour);

        tour.addDeparture(departure);

        if (request.flightIds() != null && !request.flightIds().isEmpty()) {
            List<FlightEntity> flights = flightRepository.findAllById(request.flightIds());

            if (flights.size() != request.flightIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Некоторые flightIds не существуют");
            }

            flights.forEach(departure::addFlight);
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
    public TourDepartureResponseDto update(Long id, TourDepartureUpdateRequest request) {

        TourDepartureEntity departure = tourDepartureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + id + " не найден"
                ));

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

        TourEntity oldTour = departure.getTour();

        if (!oldTour.getId().equals(newTour.getId())) {
            oldTour.removeDeparture(departure);
            newTour.addDeparture(departure);
        }

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
                    departure.addFlight(f);
                }
            }
        }

        tourDepartureMapper.updateEntity(request, newTour, departure);

        departure = tourDepartureRepository.save(departure);
        return tourDepartureMapper.toDto(departure);
    }

    @Transactional
    public void delete(Long id) {
        TourDepartureEntity departure = tourDepartureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Вылет тура с id=" + id + " не найден"
                ));

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
}
