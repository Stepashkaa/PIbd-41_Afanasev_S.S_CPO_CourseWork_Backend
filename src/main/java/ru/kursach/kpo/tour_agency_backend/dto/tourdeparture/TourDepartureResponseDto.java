package ru.kursach.kpo.tour_agency_backend.dto.tourdeparture;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "Вылет тура (ответ)")
public record TourDepartureResponseDto(

        @Schema(description = "ID вылета тура", example = "1")
        Long id,

        @Schema(description = "Дата начала", example = "2026-06-01")
        LocalDate startDate,

        @Schema(description = "Дата окончания", example = "2026-06-07")
        LocalDate endDate,

        @Schema(description = "Общая вместимость", example = "30")
        Integer capacityTotal,

        @Schema(description = "Забронированные места", example = "10")
        Integer capacityReserved,

        @Schema(description = "Переопределённая цена", example = "60000.00")
        BigDecimal priceOverride,

        @Schema(description = "Статус вылета", example = "PLANNED")
        TourDepartureStatus status,

        @Schema(description = "ID тура", example = "1")
        Long tourId,

        @Schema(description = "Название тура", example = "Париж на выходные")
        String tourTitle,

        @Schema(description = "Список ID рейсов", example = "[1, 2]")
        List<Long> flightIds
) {}
