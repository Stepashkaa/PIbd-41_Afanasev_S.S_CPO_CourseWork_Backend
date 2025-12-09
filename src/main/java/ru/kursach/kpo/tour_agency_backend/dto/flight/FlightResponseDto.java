package ru.kursach.kpo.tour_agency_backend.dto.flight;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.kursach.kpo.tour_agency_backend.model.enums.FlightStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Рейс (ответ)")
public record FlightResponseDto(

        @Schema(description = "ID рейса", example = "1")
        Long id,

        @Schema(description = "Номер рейса", example = "SU100")
        String flightNumber,

        @Schema(description = "Авиакомпания", example = "Аэрофлот")
        String carrier,

        @Schema(description = "Время вылета", example = "2025-12-31T10:00:00")
        LocalDateTime departAt,

        @Schema(description = "Время прилёта", example = "2025-12-31T12:30:00")
        LocalDateTime arriveAt,

        @Schema(description = "Статус рейса", example = "SCHEDULED")
        FlightStatus status,

        @Schema(description = "Базовая цена", example = "15000.00")
        BigDecimal basePrice,

        @Schema(description = "ID аэропорта вылета", example = "1")
        Long departureAirportId,

        @Schema(description = "Название аэропорта вылета", example = "Шереметьево")
        String departureAirportName,

        @Schema(description = "ID аэропорта прилёта", example = "2")
        Long arrivalAirportId,

        @Schema(description = "Название аэропорта прилёта", example = "Пулково")
        String arrivalAirportName
) {}
