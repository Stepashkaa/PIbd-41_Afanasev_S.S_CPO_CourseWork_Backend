package ru.kursach.kpo.tour_agency_backend.dto.airport;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на создание аэропорта")
public record AirportCreateRequest(

        @Schema(description = "IATA-код аэропорта", example = "SVO")
        @NotBlank(message = "IATA-код не может быть пустым")
        @Size(max = 10, message = "IATA-код не должен превышать 10 символов")
        String iataCode,

        @Schema(description = "Название аэропорта", example = "Шереметьево")
        @NotBlank(message = "Название аэропорта не может быть пустым")
        @Size(max = 150, message = "Название не должно превышать 150 символов")
        String name,

        @Schema(description = "Идентификатор города", example = "1")
        @NotNull(message = "Идентификатор города обязателен")
        Long cityId
) {}
