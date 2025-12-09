package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;
import ru.kursach.kpo.tour_agency_backend.service.entity.TourDepartureService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tour-departures")
@RequiredArgsConstructor
@Tag(name = "Вылеты туров (TourDeparture)")
@SecurityRequirement(name = "bearerAuth")
public class TourDepartureController {

    private final TourDepartureService tourDepartureService;

    @Operation(summary = "Получить список вылетов туров с пагинацией и фильтрацией")
    @GetMapping("/paged")
    public PageResponseDto<TourDepartureResponseDto> getAllPaged(
            @RequestParam(name = "tourId", required = false) Long tourId,
            @RequestParam(name = "status", required = false) TourDepartureStatus status,
            @RequestParam(name = "startFrom", required = false) LocalDate startFrom,
            @RequestParam(name = "startTo", required = false) LocalDate startTo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourDepartureService.getAllPaged(tourId, status, startFrom, startTo, page, size);
    }

    @Operation(summary = "Поиск вылетов по туру (с пагинацией)")
    @GetMapping("/search/by-tour")
    public PageResponseDto<TourDepartureResponseDto> searchByTour(
            @RequestParam(name = "tourId") Long tourId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourDepartureService.searchByTourId(tourId, page, size);
    }

    @Operation(summary = "Создать вылет тура")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TourDepartureResponseDto create(@RequestBody @Valid TourDepartureCreateRequest request) {
        return tourDepartureService.create(request);
    }

    @Operation(summary = "Получить вылет тура по id")
    @GetMapping("/{id}")
    public TourDepartureResponseDto getById(@PathVariable Long id) {
        return tourDepartureService.getById(id);
    }

    @Operation(summary = "Получить список всех вылетов туров")
    @GetMapping
    public List<TourDepartureResponseDto> getAll() {
        return tourDepartureService.getAll();
    }

    @Operation(summary = "Обновить вылет тура")
    @PutMapping("/{id}")
    public TourDepartureResponseDto update(@PathVariable Long id,
                                           @RequestBody @Valid TourDepartureUpdateRequest request) {
        return tourDepartureService.update(id, request);
    }

    @Operation(summary = "Удалить вылет тура")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tourDepartureService.delete(id);
    }
}
