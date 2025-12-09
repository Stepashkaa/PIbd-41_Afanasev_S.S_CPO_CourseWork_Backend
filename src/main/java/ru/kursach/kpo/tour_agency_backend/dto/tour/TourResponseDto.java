package ru.kursach.kpo.tour_agency_backend.dto.tour;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;

import java.math.BigDecimal;

@Builder
@Schema(description = "Тур (ответ)")
public record TourResponseDto(

        @Schema(description = "ID тура", example = "1")
        Long id,

        @Schema(description = "Название тура", example = "Париж на выходные")
        String title,

        @Schema(description = "Описание тура")
        String description,

        @Schema(description = "Длительность тура в днях", example = "3")
        Integer durationDays,

        @Schema(description = "Базовая цена тура", example = "45000.00")
        BigDecimal basePrice,

        @Schema(description = "Статус тура", example = "PUBLISHED")
        TourStatus status,

        @Schema(description = "Активен ли тур", example = "true")
        Boolean active,

        @Schema(description = "ID базового города", example = "1")
        Long baseCityId,

        @Schema(description = "Название города", example = "Москва")
        String baseCityName,

        @Schema(description = "ID менеджера тура", example = "10")
        Long managerUserId,

        @Schema(description = "Имя менеджера тура", example = "ivan_petrov")
        String managerUsername
) {}