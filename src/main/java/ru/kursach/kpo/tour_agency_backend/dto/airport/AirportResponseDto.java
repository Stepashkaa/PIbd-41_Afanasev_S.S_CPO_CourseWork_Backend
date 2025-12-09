package ru.kursach.kpo.tour_agency_backend.dto.airport;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Аэропорт (ответ)")
public record AirportResponseDto(

        @Schema(description = "Идентификатор аэропорта", example = "1")
        Long id,

        @Schema(description = "IATA-код", example = "SVO")
        String iataCode,

        @Schema(description = "Название аэропорта", example = "Шереметьево")
        String name,

        @Schema(description = "Идентификатор города", example = "1")
        Long cityId,

        @Schema(description = "Название города", example = "Москва")
        String cityName
) {}