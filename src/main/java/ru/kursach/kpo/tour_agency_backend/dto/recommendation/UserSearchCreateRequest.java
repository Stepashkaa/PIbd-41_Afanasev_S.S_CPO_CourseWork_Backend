package ru.kursach.kpo.tour_agency_backend.dto.recommendation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserSearchCreateRequest(
        String title,
        Long destinationCityId,    // куда хочет (может быть null)
        LocalDate dateFrom,        // желаемая дата начала
        LocalDate dateTo,          // желаемая дата окончания
        @Min(1)
        Integer personsCount,      // сколько людей
        @PositiveOrZero
        BigDecimal budgetMin,      // бюджет на человека/или общий — см. ниже (мы считаем общий)
        @PositiveOrZero
        BigDecimal budgetMax,
        String preferencesJson     // опционально
) {}
