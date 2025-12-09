package ru.kursach.kpo.tour_agency_backend.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

import java.math.BigDecimal;

@Schema(description = "Запрос на обновление бронирования")
public record BookingUpdateRequest(

        @Schema(description = "Количество человек", example = "3")
        @NotNull(message = "Количество человек обязательно")
        @Min(value = 1, message = "Количество человек должно быть не меньше 1")
        Integer personsCount,

        @Schema(description = "Итоговая стоимость", example = "60000.00")
        @NotNull(message = "Сумма бронирования обязательна")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Стоимость должна быть положительной")
        BigDecimal totalPrice,

        @Schema(description = "Статус брони", example = "CONFIRMED")
        @NotNull(message = "Статус брони обязателен")
        BookingStatus status,

        @Schema(description = "ID вылета тура", example = "1")
        @NotNull(message = "Вылет тура обязателен")
        Long tourDepartureId,

        @Schema(description = "ID выбранного рейса (может быть null)", example = "10")
        Long selectedFlightId,

        @Schema(description = "ID пользователя", example = "5")
        @NotNull(message = "Пользователь обязателен")
        Long userId
) {}