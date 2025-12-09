package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tourdeparture.TourDepartureUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;

import java.util.List;

@Component
public class TourDepartureMapper {

    public TourDepartureEntity toEntity(TourDepartureCreateRequest request,
                                        TourEntity tour) {
        return TourDepartureEntity.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .capacityTotal(request.capacityTotal())
                .capacityReserved(
                        request.capacityReserved() != null ? request.capacityReserved() : 0
                )
                .priceOverride(request.priceOverride())
                .tour(tour)
                .build();
    }

    public void updateEntity(TourDepartureUpdateRequest request,
                             TourEntity tour,
                             TourDepartureEntity entity) {
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setCapacityTotal(request.capacityTotal());
        entity.setCapacityReserved(request.capacityReserved());
        entity.setPriceOverride(request.priceOverride());
        entity.setStatus(request.status());
        entity.setTour(tour);
    }

    public TourDepartureResponseDto toDto(TourDepartureEntity entity) {
        List<Long> flightIds = entity.getFlights().stream()
                .map(FlightEntity::getId)
                .toList();

        return TourDepartureResponseDto.builder()
                .id(entity.getId())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .capacityTotal(entity.getCapacityTotal())
                .capacityReserved(entity.getCapacityReserved())
                .priceOverride(entity.getPriceOverride())
                .status(entity.getStatus())
                .tourId(entity.getTour().getId())
                .tourTitle(entity.getTour().getTitle())
                .flightIds(flightIds)
                .build();
    }
}
