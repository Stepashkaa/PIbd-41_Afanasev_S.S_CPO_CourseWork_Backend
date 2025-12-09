package ru.kursach.kpo.tour_agency_backend.dto.tourdeparture;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Запрос на обновление вылета тура (TourDeparture)")
public record TourDepartureUpdateRequest(

        @Schema(description = "Дата начала тура", example = "2026-06-01")
        @NotNull(message = "Дата начала тура обязательна")
        LocalDate startDate,

        @Schema(description = "Дата окончания тура", example = "2026-06-07")
        @NotNull(message = "Дата окончания тура обязательна")
        LocalDate endDate,

        @Schema(description = "Общая вместимость", example = "30")
        @NotNull(message = "Вместимость обязательна")
        @Min(value = 1, message = "Вместимость должна быть >= 1")
        Integer capacityTotal,

        @Schema(description = "Количество забронированных мест", example = "10")
        @NotNull(message = "Количество забронированных мест обязательно")
        @Min(value = 0, message = "Забронированных мест не может быть меньше 0")
        Integer capacityReserved,

        @Schema(description = "Переопределённая цена для этого вылета", example = "60000.00")
        @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть положительной")
        BigDecimal priceOverride,

        @Schema(description = "Статус вылета", example = "PLANNED")
        @NotNull(message = "Статус обязателен")
        TourDepartureStatus status,

        @Schema(description = "ID тура", example = "1")
        @NotNull(message = "ID тура обязателен")
        Long tourId,

        @Schema(description = "Список ID рейсов, привязанных к этому вылету", example = "[1, 2]")
        List<Long> flightIds
) {}