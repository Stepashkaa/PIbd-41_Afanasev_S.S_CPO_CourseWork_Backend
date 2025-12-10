package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.service.entity.FlightService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Рейсы")
@SecurityRequirement(name = "bearerAuth")
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/for-departure/{departureId}")
    public PageResponseDto<FlightResponseDto> getFlightsForDeparture(
            @PathVariable Long departureId,
            @RequestParam(name = "flightNumber", required = false) String flightNumber,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return flightService.getFlightsForDeparture(departureId, flightNumber, page, size);
    }

    @Operation(summary = "Получить список рейсов, подходящих для базового города тура")
    @GetMapping("/for-tour/{tourId}")
    public PageResponseDto<FlightResponseDto> getFlightsForTourBaseCity(
            @PathVariable Long tourId,
            @RequestParam(name = "flightNumber", required = false) String flightNumber,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return flightService.getFlightsForTourBaseCity(tourId, flightNumber, page, size);
    }
    @Operation(summary = "Получить список рейсов с фильтрацией и пагинацией")
    @GetMapping("/paged")
    public PageResponseDto<FlightResponseDto> getAllPaged(
            @RequestParam(name = "flightNumber", required = false) String flightNumber,
            @RequestParam(name = "departureAirportName", required = false) String departureAirportName,
            @RequestParam(name = "arrivalAirportName", required = false) String arrivalAirportName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return flightService.getAllPaged(
                flightNumber,
                departureAirportName,
                arrivalAirportName,
                page,
                size
        );
    }

    @Operation(summary = "Найти рейс по точному номеру рейса")
    @GetMapping("/search")
    public FlightResponseDto search(@RequestParam String number) {
        return flightService.findByFlightNumber(number);
    }

    @Operation(summary = "Создать рейс")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponseDto create(@RequestBody @Valid FlightCreateRequest request) {
        return flightService.create(request);
    }

    @Operation(summary = "Получить рейс по id")
    @GetMapping("/{id}")
    public FlightResponseDto getById(@PathVariable Long id) {
        return flightService.getById(id);
    }

    @Operation(summary = "Получить список всех рейсов")
    @GetMapping
    public List<FlightResponseDto> getAll() {
        return flightService.getAll();
    }

    @Operation(summary = "Обновить рейс")
    @PutMapping("/{id}")
    public FlightResponseDto update(@PathVariable Long id,
                                    @RequestBody @Valid FlightUpdateRequest request) {
        return flightService.update(id, request);
    }

    @Operation(summary = "Удалить рейс")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        flightService.delete(id);
    }

    @Operation(summary = "Привязать вылет тура к рейсу")
    @PostMapping("/{flightId}/departures/{departureId}")
    public FlightResponseDto addDeparture(
            @PathVariable Long flightId,
            @PathVariable Long departureId
    ) {
        return flightService.addDepartureToFlight(flightId, departureId);
    }

    @Operation(summary = "Отвязать вылет тура от рейса")
    @DeleteMapping("/{flightId}/departures/{departureId}")
    public FlightResponseDto removeDeparture(
            @PathVariable Long flightId,
            @PathVariable Long departureId
    ) {
        return flightService.removeDepartureFromFlight(flightId, departureId);
    }
}
