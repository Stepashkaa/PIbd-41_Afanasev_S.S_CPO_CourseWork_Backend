package ru.kursach.kpo.tour_agency_backend.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Бронирование (ответ)")
public record BookingResponseDto(

        @Schema(description = "ID бронирования", example = "100")
        Long id,

        @Schema(description = "Количество человек", example = "2")
        Integer personsCount,

        @Schema(description = "Итоговая стоимость", example = "45000.00")
        BigDecimal totalPrice,

        @Schema(description = "Статус брони", example = "PENDING")
        BookingStatus status,

        @Schema(description = "Дата создания", example = "2025-12-31T10:15:00")
        LocalDateTime createdAt,

        @Schema(description = "ID пользователя", example = "5")
        Long userId,

        @Schema(description = "Email пользователя", example = "user@example.com")
        String userEmail,

        @Schema(description = "ID вылета тура", example = "1")
        Long tourDepartureId,

        @Schema(description = "Название тура", example = "Париж на выходные")
        String tourTitle,

        @Schema(description = "ID выбранного рейса", example = "10")
        Long selectedFlightId,

        @Schema(description = "Номер рейса", example = "SU100")
        String selectedFlightNumber
) {}
