package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.flight.FlightUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;

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

    public FlightResponseDto toDto(FlightEntity entity) {
        return FlightResponseDto.builder()
                .id(entity.getId())
                .flightNumber(entity.getFlightNumber())
                .carrier(entity.getCarrier())
                .departAt(entity.getDepartAt())
                .arriveAt(entity.getArriveAt())
                .status(entity.getStatus())
                .basePrice(entity.getBasePrice())
                .departureAirportId(entity.getDepartureAirport().getId())
                .departureAirportName(entity.getDepartureAirport().getName())
                .arrivalAirportId(entity.getArrivalAirport().getId())
                .arrivalAirportName(entity.getArrivalAirport().getName())
                .build();
    }
}
