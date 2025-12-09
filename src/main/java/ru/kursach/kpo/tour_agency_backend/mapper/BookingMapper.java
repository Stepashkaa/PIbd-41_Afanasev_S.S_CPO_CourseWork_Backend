package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.booking.BookingUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.BookingEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

import java.math.BigDecimal;

@Component
public class BookingMapper {

    public BookingEntity toEntity(BookingCreateRequest request,
                                  BigDecimal totalPriceCalculated) {

        return BookingEntity.builder()
                .personsCount(request.personsCount())
                .totalPrice(totalPriceCalculated)
                .build();
    }

    public void updateEntity(BookingUpdateRequest request,
                             BigDecimal totalPriceCalculated,
                             BookingEntity entity) {

        entity.setPersonsCount(request.personsCount());
        entity.setTotalPrice(totalPriceCalculated);
        entity.setStatus(request.status());
    }

    public BookingResponseDto toDto(BookingEntity entity) {
        return BookingResponseDto.builder()
                .id(entity.getId())
                .personsCount(entity.getPersonsCount())
                .totalPrice(entity.getTotalPrice())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .tourDepartureId(entity.getTourDeparture() != null
                        ? entity.getTourDeparture().getId()
                        : null)
                .tourTitle(entity.getTourDeparture() != null &&
                        entity.getTourDeparture().getTour() != null
                        ? entity.getTourDeparture().getTour().getTitle()
                        : null)
                .selectedFlightId(entity.getSelectedFlight() != null
                        ? entity.getSelectedFlight().getId()
                        : null)
                .selectedFlightNumber(entity.getSelectedFlight() != null
                        ? entity.getSelectedFlight().getFlightNumber()
                        : null)
                .build();
    }
}
