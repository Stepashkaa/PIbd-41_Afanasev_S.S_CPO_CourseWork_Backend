package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;

@Component
public class AirportMapper {

    public AirportEntity toEntity(AirportCreateRequest request, CityEntity city) {
        return AirportEntity.builder()
                .iataCode(request.iataCode())
                .name(request.name())
                .city(city)
                .build();
    }

    public void updateEntity(AirportUpdateRequest request, AirportEntity entity) {
        entity.setIataCode(request.iataCode());
        entity.setName(request.name());
    }

    public AirportResponseDto toDto(AirportEntity entity) {
        return AirportResponseDto.builder()
                .id(entity.getId())
                .iataCode(entity.getIataCode())
                .name(entity.getName())
                .cityId(entity.getCity().getId())
                .cityName(entity.getCity().getName())
                .build();
    }
}