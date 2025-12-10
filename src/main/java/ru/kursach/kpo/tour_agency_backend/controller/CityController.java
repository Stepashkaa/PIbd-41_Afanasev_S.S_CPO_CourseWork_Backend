package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.service.entity.CityService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
@RequiredArgsConstructor
@Tag(name = "Города")
@SecurityRequirement(name = "bearerAuth")
public class CityController {

    private final CityService cityService;

    @Operation(summary = "Найти город по точному названию и стране")
    @GetMapping("/search")
    public CityResponseDto findByNameAndCountry(
            @RequestParam("name") String name,
            @RequestParam("country") String country
    ) {
        return cityService.findByNameAndCountry(name, country);
    }

    @Operation(summary = "Свободный поиск городов по одной строке (имя или страна)")
    @GetMapping("/search-free")
    public PageResponseDto<CityResponseDto> searchFree(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return cityService.searchFree(q, page, size);
    }

    @Operation(summary = "Получить список городов с фильтрацией и пагинацией")
    @GetMapping("/paged")
    public PageResponseDto<CityResponseDto> getAllPaged(
            @RequestParam(name = "name", required = false)
            String name,

            @RequestParam(name = "country", required = false)
            String country,

            @RequestParam(name = "page", defaultValue = "0")
            int page,

            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE)
            int size
    ) {
        return cityService.getAllPaged(name, country, page, size);
    }

    @Operation(summary = "Создать город")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponseDto create(@RequestBody @Valid CityCreateRequest request) {
        return cityService.create(request);
    }

    @Operation(summary = "Получить город по id")
    @GetMapping("/{id}")
    public CityResponseDto getById(@PathVariable Long id) {
        return cityService.getById(id);
    }

    @Operation(summary = "Получить список всех городов")
    @GetMapping
    public List<CityResponseDto> getAll() {
        return cityService.getAll();
    }

    @Operation(summary = "Обновить город")
    @PutMapping("/{id}")
    public CityResponseDto update(@PathVariable Long id,
                                  @RequestBody @Valid CityUpdateRequest request) {
        return cityService.update(id, request);
    }

    @Operation(summary = "Удалить город")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cityService.delete(id);
    }
}
