package ru.kursach.kpo.tour_agency_backend.dto.city;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Город (ответ)")
public record CityResponseDto (

        @Schema(description = "Идентификатор города", example = "1")
        Long id,

        @Schema(description = "Название города", example = "Москва")
        String name,

        @Schema(description = "Страна", example = "Россия")
        String country,

        @Schema(description = "Часовой пояс", example = "Europe/Moscow")
        String timezone
) {}

