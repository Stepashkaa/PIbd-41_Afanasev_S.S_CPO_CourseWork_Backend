package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;

import java.util.List;

@Component
public class FlightMapper {

    public FlightEntity toEntity(FlightCreateRequest request,
                                 AirportEntity departureAirport,
                                 AirportEntity arrivalAirport) {
        return FlightEntity.builder()
                .flightNumber(request.flightNumber())
                .carrier(request.carrier())
                .departAt(request.departAt())
                .arriveAt(request.arriveAt())
                .basePrice(request.basePrice())
                .departureAirport(departureAirport)
                .arrivalAirport(arrivalAirport)
                .build();
    }

    public void updateEntity(FlightUpdateRequest request,
                             FlightEntity entity) {
        entity.setFlightNumber(request.flightNumber());
        entity.setCarrier(request.carrier());
        entity.setDepartAt(request.departAt());
        entity.setArriveAt(request.arriveAt());
        entity.setStatus(request.status());
        entity.setBasePrice(request.basePrice());
    }

    public FlightResponseDto toDto(FlightEntity flight) {
        return FlightResponseDto.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .carrier(flight.getCarrier())
                .departAt(flight.getDepartAt())
                .arriveAt(flight.getArriveAt())
                .status(flight.getStatus())
                .basePrice(flight.getBasePrice())
                .departureAirportId(flight.getDepartureAirport().getId())
                .departureAirportName(flight.getDepartureAirport().getName())
                .arrivalAirportId(flight.getArrivalAirport().getId())
                .arrivalAirportName(flight.getArrivalAirport().getName())
                .tourDepartureIds(
                        flight.getTourDepartures() == null ? List.of() :
                                flight.getTourDepartures().stream().map(TourDepartureEntity::getId).toList()
                )
                .build();
    }
}
