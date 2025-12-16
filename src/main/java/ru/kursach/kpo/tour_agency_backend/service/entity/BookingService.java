package ru.kursach.kpo.tour_agency_backend.service.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingStatusUpdateRequest;
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
    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional
    public BookingResponseDto cancelMy(Long id, String email) {
        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бронь не найдена"));

        if (!booking.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваша бронь");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Отменить можно только PENDING");
        }

        // освободить места, если статус занимал места
        if (isStatusCounting(booking.getStatus())) {
            TourDepartureEntity dep = booking.getTourDeparture();
            dep.setCapacityReserved(dep.getCapacityReserved() - booking.getPersonsCount());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return bookingMapper.toDto(booking);
    }


    @Transactional(readOnly = true)
    public Page<BookingEntity> getAllPaged(Long userId,
                                           Long tourDepartureId,
                                           BookingStatus status,
                                           LocalDateTime createdFrom,
                                           LocalDateTime createdTo,
                                           String userEmail,
                                           Pageable pageable) {

        userEmail = trimToNull(userEmail); // ✅ пустое -> null

        return bookingRepository.searchPaged(
                userId,
                tourDepartureId,
                status,
                createdFrom,
                createdTo,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public PageResponseDto<BookingResponseDto> getAllPaged(
            Long userId,
            Long tourDepartureId,
            BookingStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            int page,
            int size
    ) {
        // Обработка пустой строки

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
        return getAllPaged(null, null, null, null, null, page, size);
    }

    @Transactional
    public BookingResponseDto create(BookingCreateRequest request, String userEmailFromToken) {

        if (userEmailFromToken == null || userEmailFromToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Не удалось определить пользователя из токена");
        }

        UserEntity user = userRepository.findByEmail(userEmailFromToken)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Пользователь с email=" + userEmailFromToken + " не найден"
                ));

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

        // ✅ рейс туда обязателен
        FlightEntity outbound = requireFlight(request.outboundFlightId(), "Туда");
        validateFlightBelongsToDeparture(outbound, tourDeparture, "Туда");

        // ✅ рейс обратно опционален
        FlightEntity ret = null;
        if (request.returnFlightId() != null) {
            ret = requireFlight(request.returnFlightId(), "Обратно");
            validateFlightBelongsToDeparture(ret, tourDeparture, "Обратно");

            if (ret.getId().equals(outbound.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Нельзя выбрать один и тот же рейс туда и обратно");
            }
        }

        BigDecimal totalPriceCalculated = calculateTotalPrice(tourDeparture, persons, outbound, ret);

        BookingEntity booking = BookingEntity.builder()
                .personsCount(persons)
                .totalPrice(totalPriceCalculated)
                .status(BookingStatus.PENDING)
                .user(user)
                .tourDeparture(tourDeparture)
                .outboundFlight(outbound)
                .returnFlight(ret)
                .build();

        if (isStatusCounting(booking.getStatus())) {
            tourDeparture.setCapacityReserved(
                    tourDeparture.getCapacityReserved() + persons
            );
            syncSalesClosedStatus(tourDeparture);
        }

        // места учитываем как и раньше
        // связи (как у тебя)
        user.addBooking(booking);
        tourDeparture.addBooking(booking);
        outbound.addOutboundBooking(booking);
        if (ret != null) ret.addReturnBooking(booking);

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
    public BookingResponseDto updateStatus(Long id, BookingStatusUpdateRequest request) {

        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Бронирование с id=" + id + " не найдено"
                ));

        BookingStatus oldStatus = booking.getStatus();
        BookingStatus newStatus = request.status();

        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Статус обязателен");
        }

        // ничего не делаем
        if (oldStatus == newStatus) {
            return bookingMapper.toDto(booking);
        }

        TourDepartureEntity departure = booking.getTourDeparture();
        if (departure == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У бронирования не задан вылет тура");
        }

        // На всякий случай: если вылет уже “закрыт” — запрещаем смены на counting-статусы
        // (можно ослабить правило, если хочешь)
        if (isStatusCounting(newStatus)) {
            validateDepartureForBooking(departure);
        }

        int persons = booking.getPersonsCount() != null ? booking.getPersonsCount() : 0;
        if (persons <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректное количество человек в бронировании");
        }

        boolean oldCounting = isStatusCounting(oldStatus);
        boolean newCounting = isStatusCounting(newStatus);

        // 1) освобождаем места, если раньше статус занимал места
        if (oldCounting && !newCounting) {
            departure.setCapacityReserved(departure.getCapacityReserved() - persons);
        }

        // 2) занимаем места, если новый статус занимает места
        if (!oldCounting && newCounting) {
            int available = departure.getCapacityTotal() - departure.getCapacityReserved();
            if (persons > available) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Нельзя перевести бронь в статус " + newStatus +
                                ": не хватает мест (нужно " + persons + ", доступно " + available + ")"
                );
            }
            departure.setCapacityReserved(departure.getCapacityReserved() + persons);
        }
        syncSalesClosedStatus(departure);

        booking.setStatus(newStatus);

        booking = bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }

    @Transactional
    public BookingResponseDto update(Long id, BookingUpdateRequest request) {

        BookingEntity booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Бронирование с id=" + id + " не найдено"
                ));

        // --- new refs
        UserEntity newUser = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Пользователь с id=" + request.userId() + " не найден"
                ));

        TourDepartureEntity newDeparture = tourDepartureRepository.findById(request.tourDepartureId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Вылет тура с id=" + request.tourDepartureId() + " не найден"
                ));

        validateDepartureForBooking(newDeparture);

        int newPersons = request.personsCount();
        if (newPersons <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Количество человек должно быть больше 0");
        }

        BookingStatus newStatus = request.status();

        // ✅ outbound обязателен
        FlightEntity newOutbound = requireFlight(request.outboundFlightId(), "Туда");
        validateFlightBelongsToDeparture(newOutbound, newDeparture, "Туда");

        // ✅ return опционален
        FlightEntity newReturn = null;
        if (request.returnFlightId() != null) {
            newReturn = requireFlight(request.returnFlightId(), "Обратно");
            validateFlightBelongsToDeparture(newReturn, newDeparture, "Обратно");

            if (newReturn.getId().equals(newOutbound.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Нельзя выбрать один и тот же рейс туда и обратно");
            }
        }

        // --- old refs/values
        UserEntity oldUser = booking.getUser();
        TourDepartureEntity oldDeparture = booking.getTourDeparture();
        FlightEntity oldOutbound = booking.getOutboundFlight();
        FlightEntity oldReturn = booking.getReturnFlight();

        int oldPersons = booking.getPersonsCount();
        BookingStatus oldStatus = booking.getStatus();

        // --- capacity accounting
        if (oldDeparture != null && isStatusCounting(oldStatus)) {
            int updated = oldDeparture.getCapacityReserved() - oldPersons;
            oldDeparture.setCapacityReserved(Math.max(0, updated));
            syncSalesClosedStatus(oldDeparture);
        }

        if (isStatusCounting(newStatus)) {
            int available = newDeparture.getCapacityTotal() - newDeparture.getCapacityReserved();
            if (newPersons > available) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Нельзя забронировать " + newPersons + " мест: превышена вместимость вылета"
                );
            }
            newDeparture.setCapacityReserved(newDeparture.getCapacityReserved() + newPersons);
            syncSalesClosedStatus(newDeparture);
        }

        // --- total price (с учётом рейсов)
        BigDecimal totalPriceCalculated = calculateTotalPrice(newDeparture, newPersons, newOutbound, newReturn);

        // --- relations: user
        if (oldUser == null || !oldUser.getId().equals(newUser.getId())) {
            if (oldUser != null) oldUser.removeBooking(booking);
            newUser.addBooking(booking);
        }

        // --- relations: departure
        if (oldDeparture == null || !oldDeparture.getId().equals(newDeparture.getId())) {
            if (oldDeparture != null) oldDeparture.removeBooking(booking);
            newDeparture.addBooking(booking);
        }

        // --- relations: outbound flight (обязателен)
        if (oldOutbound == null || !oldOutbound.getId().equals(newOutbound.getId())) {
            if (oldOutbound != null) oldOutbound.removeOutboundBooking(booking);
            newOutbound.addOutboundBooking(booking);
        }

        // --- relations: return flight (опционален)
        // 1) если был старый return и он поменялся/убран -> снимаем
        if (oldReturn != null && (newReturn == null || !oldReturn.getId().equals(newReturn.getId()))) {
            oldReturn.removeReturnBooking(booking);
        }
        // 2) если новый return задан и он новый -> ставим
        if (newReturn != null && (oldReturn == null || !newReturn.getId().equals(oldReturn.getId()))) {
            newReturn.addReturnBooking(booking);
        }
        // 3) если новый return == null -> гарантированно обнуляем поле (на случай, если removeReturnBooking не вызвался)
        if (newReturn == null) {
            booking.setReturnFlight(null);
        }

        // --- apply simple fields
        booking.setPersonsCount(newPersons);
        booking.setTotalPrice(totalPriceCalculated);
        booking.setStatus(newStatus);

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
        FlightEntity outbound = booking.getOutboundFlight();
        FlightEntity ret = booking.getReturnFlight();

        // capacity back (только если статус учитывался)
        if (departure != null && isStatusCounting(booking.getStatus())) {
            int updated = departure.getCapacityReserved() - booking.getPersonsCount();
            departure.setCapacityReserved(Math.max(0, updated));
            syncSalesClosedStatus(departure);
        }

        // unlink relations (важен порядок — сначала связи, потом delete)
        if (user != null) user.removeBooking(booking);
        if (departure != null) departure.removeBooking(booking);

        if (outbound != null) outbound.removeOutboundBooking(booking);
        if (ret != null) ret.removeReturnBooking(booking);

        try {
            bookingRepository.delete(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Невозможно удалить бронирование");
        }
    }


    /**
     * Расчёт итоговой стоимости брони:
     * pricePerPerson = priceOverride (если не null) иначе tour.basePrice
     * totalPrice = pricePerPerson * personsCount
     */
    private BigDecimal calculateTotalPrice(
            TourDepartureEntity departure,
            int personsCount,
            FlightEntity outbound,
            FlightEntity ret
    ) {
        BigDecimal pricePerPerson = departure.getPriceOverride() != null
                ? departure.getPriceOverride()
                : departure.getTour().getBasePrice();

        BigDecimal flights = outbound.getBasePrice();
        if (ret != null) flights = flights.add(ret.getBasePrice());

        BigDecimal perPerson = pricePerPerson.add(flights);
        return perPerson.multiply(BigDecimal.valueOf(personsCount));
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

    private FlightEntity requireFlight(Long flightId, String label) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        label + " рейс с id=" + flightId + " не найден"
                ));
    }

    private void validateFlightBelongsToDeparture(FlightEntity flight, TourDepartureEntity dep, String label) {
        if (!flight.getTourDepartures().contains(dep)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " рейс id=" + flight.getId() + " не относится к вылету тура id=" + dep.getId()
            );
        }
    }
    private void syncSalesClosedStatus(TourDepartureEntity dep) {
        if (dep == null) return;

        int reserved = dep.getCapacityReserved() != null ? dep.getCapacityReserved() : 0;
        int total = dep.getCapacityTotal() != null ? dep.getCapacityTotal() : 0;

        // если места кончились -> закрываем продажи
        if (total > 0 && reserved >= total) {
            if (dep.getStatus() == TourDepartureStatus.PLANNED) {
                dep.setStatus(TourDepartureStatus.SALES_CLOSED);
            }
            return;
        }

        // если места появились обратно -> можно открыть продажи (по желанию)
        if (reserved < total) {
            if (dep.getStatus() == TourDepartureStatus.SALES_CLOSED) {
                dep.setStatus(TourDepartureStatus.PLANNED);
            }
        }
    }


}