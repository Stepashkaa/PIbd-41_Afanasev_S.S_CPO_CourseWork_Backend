package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.service.entity.AirportService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Tag(name = "Аэропорты")
@SecurityRequirement(name = "bearerAuth")
public class AirportController {

    private final AirportService airportService;

    @Operation(summary = "Получить список аэропортов с фильтрацией и пагинацией")
    @GetMapping("/paged")
    public PageResponseDto<AirportResponseDto> getAllPaged(
            @RequestParam(name = "iata", required = false) String iata,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "cityName", required = false) String cityName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return airportService.getAllPaged(iata, name, cityName, page, size);
    }

    @Operation(summary = "Найти аэропорт по точному IATA-коду")
    @GetMapping("/search/by-iata")
    public AirportResponseDto findByIata(@RequestParam("iata") String iata) {
        return airportService.findByIataCode(iata);
    }

    @Operation(summary = "Создать аэропорт")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AirportResponseDto create(@RequestBody @Valid AirportCreateRequest request) {
        return airportService.create(request);
    }

    @Operation(summary = "Получить аэропорт по id")
    @GetMapping("/{id}")
    public AirportResponseDto getById(@PathVariable Long id) {
        return airportService.getById(id);
    }

    @Operation(summary = "Получить список всех аэропортов")
    @GetMapping
    public List<AirportResponseDto> getAll() {
        return airportService.getAll();
    }

    @Operation(summary = "Обновить аэропорт")
    @PutMapping("/{id}")
    public AirportResponseDto update(@PathVariable Long id,
                                     @RequestBody @Valid AirportUpdateRequest request) {
        return airportService.update(id, request);
    }

    @Operation(summary = "Удалить аэропорт")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        airportService.delete(id);
    }
}