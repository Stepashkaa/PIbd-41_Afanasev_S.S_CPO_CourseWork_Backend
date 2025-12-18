package ru.kursach.kpo.tour_agency_backend.dto.recommendation;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RecommendedTourCardDto(
        Long recommendationId,     // чтобы отметить selected
        Double score,

        Long tourId,
        String title,
        String description,
        Integer durationDays,
        Long baseCityId,
        String baseCityName,
        BigDecimal basePrice,

        Long tourDepartureId,
        LocalDate startDate,
        LocalDate endDate,
        Integer capacityTotal,
        Integer capacityReserved,
        BigDecimal priceOverride,

        BigDecimal pricePerPerson  // (override ?? basePrice)
) {}
