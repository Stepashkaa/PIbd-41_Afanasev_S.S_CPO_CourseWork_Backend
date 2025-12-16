package ru.kursach.kpo.tour_agency_backend.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

import java.math.BigDecimal;

@Schema(description = "Запрос на создание бронирования")
public record BookingCreateRequest(

        @Schema(description = "Количество человек", example = "2")
        @NotNull(message = "Количество человек обязательно")
        @Min(value = 1, message = "Количество человек должно быть не меньше 1")
        Integer personsCount,

        @Schema(description = "Итоговая стоимость", example = "45000.00")
        @NotNull(message = "Сумма бронирования обязательна")
        @DecimalMin(value = "0.0", inclusive = false,
                message = "Стоимость должна быть положительной")
        BigDecimal totalPrice,

        @Schema(description = "ID вылета тура", example = "1")
        @NotNull(message = "Вылет тура обязателен")
        Long tourDepartureId,

        @Schema(description = "ID туда выбранного рейса (может быть null)", example = "10")
        @NotNull(message = "Рейс туда обязателен")
        Long outboundFlightId,

        @Schema(description = "ID обратно выбранного рейса (может быть null)", example = "10")
        Long returnFlightId
) {}

