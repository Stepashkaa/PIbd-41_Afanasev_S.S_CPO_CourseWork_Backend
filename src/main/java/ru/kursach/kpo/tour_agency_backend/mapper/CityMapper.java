package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;

@Component
public class CityMapper {

    public CityEntity toEntity(CityCreateRequest request) {
        return CityEntity.builder()
                .name(request.name())
                .country(request.country())
                .timezone(request.timezone())
                .build();
    }

    public void updateEntity(CityUpdateRequest request, CityEntity entity) {
        entity.setName(request.name());
        entity.setCountry(request.country());
        entity.setTimezone(request.timezone());
    }

    public CityResponseDto toDto(CityEntity entity) {
        return CityResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .country(entity.getCountry())
                .timezone(entity.getTimezone())
                .build();
    }
}