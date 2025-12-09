package ru.kursach.kpo.tour_agency_backend.dto.city;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "Запрос на создание города")
public record CityCreateRequest (

    @Schema(description = "Название города", example = "Москва")
    @NotBlank(message = "Название города не может быть пустым")
    @Size(max = 150, message = "Название города не должно превышать 150 символов")
    String name,

    @Schema(description = "Страна", example = "Россия")
    @NotBlank(message = "Страна не может быть пустой")
    @Size(max = 150, message = "Название страны не должно превышать 150 символов")
    String country,

    @Schema(description = "Часовой пояс", example = "Europe/Moscow")
    @Size(max = 50, message = "Часовой пояс не должен превышать 50 символов")
    String timezone
) {}
