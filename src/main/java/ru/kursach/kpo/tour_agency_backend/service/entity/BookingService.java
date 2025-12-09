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
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.mapper.BookingMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;
import ru.kursach.kpo.tour_agency_backend.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final FlightRepository flightRepository;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<BookingResponseDto> getAllPaged(
            Long userId,
            Long tourDepartureId,
            BookingStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            String userEmail,
            int page,
            int size
    ) {
        String emailFilter = (userEmail != null && !userEmail.isBlank())
                ? userEmail.trim()
                : null;

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending().and(Sort.by("id").descending())
        );

        Page<BookingEntity> bookingPage = bookingRepository.searchPaged(
                userId,
                tourDepartureId,
                status,
                createdFrom,
                createdTo,
                emailFilter,
                pageable
        );

        return PageResponseDto.<BookingResponseDto>builder()
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalPages(bookingPage.getTotalPages())
                .totalElements(bookingPage.getTotalElements())
                .content(bookingPage.getContent().stream()
                        .map(bookingMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<BookingResponseDto> searchByUserEmail(
            String email,
            int page,
            int size
    ) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр email обязателен для поиска бронирований по пользователю"
            );
        }

        return getAllPaged(null, null, null, null, null, email, page, size);
    }

    @Transactional
    public BookingResponseDto create(BookingCreateRequest request) {

        TourDepartureEntity tourDeparture = tourDepartureRepository
                .findById(request.tourDepartureId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Вылет тура с id=" + request.tourDepartureId() + " не найден"
                ));

        validateDepartureForBooking(tourDeparture);

        int persons = request.personsCount();
        if (persons <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Количество человек должно быть больше 0"
            );
        }

        int available = tourDeparture.getCapacityTotal() - tourDeparture.getCapacityReserved();
        if (persons > available) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя забронировать " + persons + " мест: превышена вместимость вылета"
            );
        }

        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Пользователь с id=" + request.userId() + " не найден"
                ));

        FlightEntity selectedFlight = null;
        if (request.selectedFlightId() != null) {
            selectedFlight = flightRepository.findById(request.selectedFlightId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Рейс с id=" + request.selectedFlightId() + " не найден"
                    ));

            if (!selectedFlight.getTourDepartures().contains(tourDeparture)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Рейс с id=" + request.selectedFlightId()
                                + " не относится к вылету тура id="
                                + request.tourDepartureId()
                );
            }
        }

        BigDecimal totalPriceCalculated = calculateTotalPrice(tourDeparture, persons);

        BookingEntity booking = bookingMapper.toEntity(
                request,
                totalPriceCalculated
        );

        if (isStatusCounting(booking.getStatus())) {
            tourDeparture.setCapacityReserved(
                    tourDeparture.getCapacityReserved() + persons
            );
        }

        user.addBooking(booking);
        tourDeparture.addBooking(booking);
        if (selectedFlight != null) {
            selectedFlight.addBooking(booking);
        }

        booking = bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getById(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Бронирование с id=" + id + " не найдено"
                ));
        return bookingMapper.toDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAll() {
        return bookingRepository.findAll().stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional
    public BookingResponseDto update(Long id, BookingUpdateRequest request) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Бронирование с id=" + id + " не найдено"
                ));

        UserEntity newUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Пользователь с id=" + request.userId() + " не найден"
                ));

        TourDepartureEntity newDeparture = tourDepartureRepository
                .findById(request.tourDepartureId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Вылет тура с id=" + request.tourDepartureId() + " не найден"
                ));

        validateDepartureForBooking(newDeparture);

        FlightEntity newFlight = null;
        if (request.selectedFlightId() != null) {
            newFlight = flightRepository.findById(request.selectedFlightId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Рейс с id=" + request.selectedFlightId() + " не найден"
                    ));

            if (!newFlight.getTourDepartures().contains(newDeparture)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Рейс с id=" + request.selectedFlightId()
                                + " не относится к вылету тура id="
                                + request.tourDepartureId()
                );
            }
        }

        TourDepartureEntity oldDeparture = booking.getTourDeparture();
        int oldPersons = booking.getPersonsCount();
        BookingStatus oldStatus = booking.getStatus();

        int newPersons = request.personsCount();
        BookingStatus newStatus = request.status();

        if (newPersons <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Количество человек должно быть больше 0"
            );
        }

        if (isStatusCounting(oldStatus)) {
            oldDeparture.setCapacityReserved(
                    oldDeparture.getCapacityReserved() - oldPersons
            );
        }

        if (isStatusCounting(newStatus)) {
            int available = newDeparture.getCapacityTotal() - newDeparture.getCapacityReserved();
            if (newPersons > available) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Нельзя забронировать " + newPersons
                                + " мест: превышена вместимость вылета"
                );
            }
            newDeparture.setCapacityReserved(
                    newDeparture.getCapacityReserved() + newPersons
            );
        }

        BigDecimal totalPriceCalculated = calculateTotalPrice(newDeparture, newPersons);

        UserEntity oldUser = booking.getUser();
        if (!oldUser.getId().equals(newUser.getId())) {
            oldUser.removeBooking(booking);
            newUser.addBooking(booking);
        }

        if (!oldDeparture.getId().equals(newDeparture.getId())) {
            oldDeparture.removeBooking(booking);
            newDeparture.addBooking(booking);
        }

        FlightEntity oldFlight = booking.getSelectedFlight();
        if (oldFlight != null && (newFlight == null ||
                !oldFlight.getId().equals(newFlight.getId()))) {
            oldFlight.removeBooking(booking);
        }
        if (newFlight != null &&
                (oldFlight == null || !oldFlight.getId().equals(newFlight.getId()))) {
            newFlight.addBooking(booking);
        }

        bookingMapper.updateEntity(request, totalPriceCalculated, booking);

        booking = bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }


    @Transactional
    public void delete(Long id) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Бронирование с id=" + id + " не найдено"
                ));

        UserEntity user = booking.getUser();
        TourDepartureEntity departure = booking.getTourDeparture();
        FlightEntity flight = booking.getSelectedFlight();

        // Корректируем вместимость только если статус брони учитывался
        if (departure != null && isStatusCounting(booking.getStatus())) {
            departure.setCapacityReserved(
                    departure.getCapacityReserved() - booking.getPersonsCount()
            );
        }

        if (user != null) {
            user.removeBooking(booking);
        }
        if (departure != null) {
            departure.removeBooking(booking);
        }
        if (flight != null) {
            flight.removeBooking(booking);
        }

        try {
            bookingRepository.delete(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить бронирование"
            );
        }
    }

    /**
     * Расчёт итоговой стоимости брони:
     * pricePerPerson = priceOverride (если не null) иначе tour.basePrice
     * totalPrice = pricePerPerson * personsCount
     */
    private BigDecimal calculateTotalPrice(TourDepartureEntity departure, int personsCount) {
        BigDecimal pricePerPerson = departure.getPriceOverride() != null
                ? departure.getPriceOverride()
                : departure.getTour().getBasePrice();

        return pricePerPerson.multiply(BigDecimal.valueOf(personsCount));
    }

    /**
     * Статусы, которые реально "занимают" места.
     * Остальные (например, CANCELLED) не должны учитываться в capacityReserved.
     */
    private boolean isStatusCounting(BookingStatus status) {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    /**
     * Проверка, что по данному вылету ещё можно создавать / изменять бронирования.
     */
    private void validateDepartureForBooking(TourDepartureEntity departure) {
        if (departure.getStatus() == TourDepartureStatus.CANCELLED
                || departure.getStatus() == TourDepartureStatus.COMPLETED
                || departure.getStatus() == TourDepartureStatus.SALES_CLOSED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя работать с бронированиями для вылета со статусом: " + departure.getStatus()
            );
        }

        if (departure.getStartDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Нельзя создавать/изменять бронирование на вылет, который уже начался или завершился"
            );
        }
    }
}