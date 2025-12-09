package ru.kursach.kpo.tour_agency_backend.dto.flight;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.FlightStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Запрос на создание рейса")
public record FlightCreateRequest(

        @Schema(description = "Номер рейса", example = "SU100")
        @NotBlank(message = "Номер рейса не может быть пустым")
        @Size(max = 20, message = "Номер рейса не должен превышать 20 символов")
        String flightNumber,

        @Schema(description = "Авиакомпания", example = "Аэрофлот")
        @NotBlank(message = "Название авиакомпании не может быть пустым")
        @Size(max = 150, message = "Название авиакомпании не должно превышать 150 символов")
        String carrier,

        @Schema(description = "Время вылета (LocalDateTime)", example = "2025-12-31T10:00:00")
        @NotNull(message = "Время вылета обязательно")
        LocalDateTime departAt,

        @Schema(description = "Время прилёта (LocalDateTime)", example = "2025-12-31T12:30:00")
        @NotNull(message = "Время прилёта обязательно")
        LocalDateTime arriveAt,

        @Schema(description = "Базовая цена билета", example = "15000.00")
        @NotNull(message = "Базовая цена обязательна")
        @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть положительной")
        BigDecimal basePrice,

        @Schema(description = "ID аэропорта вылета", example = "1")
        @NotNull(message = "Аэропорт вылета обязателен")
        Long departureAirportId,

        @Schema(description = "ID аэропорта прилёта", example = "2")
        @NotNull(message = "Аэропорт прилёта обязателен")
        Long arrivalAirportId
) {}