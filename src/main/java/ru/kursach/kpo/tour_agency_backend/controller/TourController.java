package ru.kursach.kpo.tour_agency_backend.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.service.entity.TourService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tours")
@RequiredArgsConstructor
@Tag(name = "Туры")
@SecurityRequirement(name = "bearerAuth")
public class TourController {

    private final TourService tourService;

    @Operation(summary = "Получить публичный тур по id")
    @GetMapping("/public/{id}")
    public TourResponseDto getPublicById(@PathVariable Long id) {
        return tourService.getById(id); // или отдельный метод, если нужно скрыть неактивные/неопубликованные
    }

    @GetMapping("/public/paged")
    public PageResponseDto<TourResponseDto> getPublicPaged(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "baseCityId", required = false) Long baseCityId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourService.getPublicPaged(title, baseCityId, page, size);
    }

    @GetMapping("/my/paged")
    public PageResponseDto<TourResponseDto> getMyPaged(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "baseCityId", required = false) Long baseCityId,
            @RequestParam(name = "status", required = false) TourStatus status,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName(); // у тебя subject = email
        return tourService.getMyPaged(email, title, baseCityId, status, active, page, size);
    }

    @Operation(summary = "Получить список туров с пагинацией и фильтрацией")
    @GetMapping("/paged")
    public PageResponseDto<TourResponseDto> getAllPaged(
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "baseCityId", required = false) Long baseCityId,
            @RequestParam(name = "status", required = false) TourStatus status,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "managerUserId", required = false) Long managerUserId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourService.getAllPaged(title, baseCityId, status, active, managerUserId, page, size);
    }

    @Operation(summary = "Поиск туров по названию (с пагинацией)")
    @GetMapping("/search")
    public PageResponseDto<TourResponseDto> searchByTitle(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourService.searchByTitle(title, page, size);
    }

    @Operation(summary = "Создать тур")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TourResponseDto create(@RequestBody @Valid TourCreateRequest request) {
        return tourService.create(request);
    }

    @Operation(summary = "Получить тур по id")
    @GetMapping("/{id}")
    public TourResponseDto getById(@PathVariable Long id) {
        return tourService.getById(id);
    }

    @Operation(summary = "Получить список всех туров (без фильтрации)")
    @GetMapping
    public List<TourResponseDto> getAll() {
        return tourService.getAll();
    }

    @Operation(summary = "Обновить тур")
    @PutMapping("/{id}")
    public TourResponseDto update(@PathVariable Long id,
                                  @RequestBody @Valid TourUpdateRequest request) {
        return tourService.update(id, request);
    }

    @Operation(summary = "Удалить тур")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tourService.delete(id);
    }
}