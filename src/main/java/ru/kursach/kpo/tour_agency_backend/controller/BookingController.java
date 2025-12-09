package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;
import ru.kursach.kpo.tour_agency_backend.service.entity.BookingService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Бронирования")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Получить список бронирований с фильтрацией и пагинацией")
    @GetMapping("/paged")
    public PageResponseDto<BookingResponseDto> getAllPaged(
            @RequestParam(name = "userId", required = false) Long userId,
            @RequestParam(name = "tourDepartureId", required = false) Long tourDepartureId,
            @RequestParam(name = "status", required = false) BookingStatus status,
            @RequestParam(name = "createdFrom", required = false) LocalDateTime createdFrom,
            @RequestParam(name = "createdTo", required = false) LocalDateTime createdTo,
            @RequestParam(name = "userEmail", required = false) String userEmail,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return bookingService.getAllPaged(
                userId,
                tourDepartureId,
                status,
                createdFrom,
                createdTo,
                userEmail,
                page,
                size
        );
    }

    @Operation(summary = "Поиск бронирований по email пользователя (с пагинацией)")
    @GetMapping("/search/by-user-email")
    public PageResponseDto<BookingResponseDto> searchByUserEmail(
            @RequestParam(name = "email") String email,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return bookingService.searchByUserEmail(email, page, size);
    }

    @Operation(summary = "Создать бронирование")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDto create(@RequestBody @Valid BookingCreateRequest request) {
        return bookingService.create(request);
    }

    @Operation(summary = "Получить бронирование по id")
    @GetMapping("/{id}")
    public BookingResponseDto getById(@PathVariable Long id) {
        return bookingService.getById(id);
    }

    @Operation(summary = "Получить список всех бронирований")
    @GetMapping
    public List<BookingResponseDto> getAll() {
        return bookingService.getAll();
    }

    @Operation(summary = "Обновить бронирование")
    @PutMapping("/{id}")
    public BookingResponseDto update(@PathVariable Long id,
                                     @RequestBody @Valid BookingUpdateRequest request) {
        return bookingService.update(id, request);
    }

    @Operation(summary = "Удалить бронирование")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookingService.delete(id);
    }
}