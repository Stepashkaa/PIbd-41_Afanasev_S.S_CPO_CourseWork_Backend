package ru.kursach.kpo.tour_agency_backend.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

@Schema(description = "Запрос на изменение статуса бронирования")
public record BookingStatusUpdateRequest(
        @NotNull BookingStatus status
) {}