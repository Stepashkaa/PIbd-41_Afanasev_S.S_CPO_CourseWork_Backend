package ru.kursach.kpo.tour_agency_backend.dto.tour;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;

import java.math.BigDecimal;

@Schema(description = "Запрос на создание тура")
public record TourCreateRequest(

        @Schema(description = "Название тура", example = "Париж на выходные")
        @NotBlank(message = "Название тура не может быть пустым")
        @Size(max = 200, message = "Название тура не должно превышать 200 символов")
        String title,

        @Schema(description = "Описание тура", example = "Экскурсии, музеи, круизы по Сене")
        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description,

        @Schema(description = "Длительность тура в днях", example = "3")
        @NotNull(message = "Длительность тура обязательна")
        @Min(value = 1, message = "Длительность тура должна быть не менее 1 дня")
        @Max(value = 365, message = "Длительность тура не должна превышать 365 дней")
        Integer durationDays,

        @Schema(description = "Базовая цена тура", example = "45000.00")
        @NotNull(message = "Базовая цена обязательна")
        @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть положительной")
        BigDecimal basePrice,

        @Schema(description = "Активен ли тур", example = "true")
        Boolean active,

        @Schema(description = "ID базового города", example = "1")
        @NotNull(message = "ID города обязателен")
        Long baseCityId,

        @Schema(description = "ID менеджера тура (пользователь с ролью MANAGER)", example = "10",
                nullable = true)
        Long managerUserId
) {}