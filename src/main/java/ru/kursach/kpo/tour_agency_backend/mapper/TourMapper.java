package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;

@Component
public class TourMapper {

    public TourEntity toEntity(TourCreateRequest request,
                               CityEntity baseCity,
                               UserEntity managerUser) {
        return TourEntity.builder()
                .title(request.title())
                .description(request.description())
                .durationDays(request.durationDays())
                .basePrice(request.basePrice())
                .active(Boolean.TRUE.equals(request.active()))
                .baseCity(baseCity)
                .managerUser(managerUser)
                .build();
    }

    public void updateEntity(TourUpdateRequest request,
                             CityEntity baseCity,
                             UserEntity managerUser,
                             TourEntity entity) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setDurationDays(request.durationDays());
        entity.setBasePrice(request.basePrice());
        entity.setStatus(request.status());
        entity.setActive(Boolean.TRUE.equals(request.active()));
        entity.setBaseCity(baseCity);
        entity.setManagerUser(managerUser);
    }

    public TourResponseDto toDto(TourEntity entity) {
        return TourResponseDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .durationDays(entity.getDurationDays())
                .basePrice(entity.getBasePrice())
                .status(entity.getStatus())
                .active(entity.isActive())
                .baseCityId(entity.getBaseCity().getId())
                .baseCityName(entity.getBaseCity().getName())
                .managerUserId(entity.getManagerUser() != null ? entity.getManagerUser().getId() : null)
                .managerUsername(entity.getManagerUser() != null ? entity.getManagerUser().getUsername() : null)
                .build();
    }
}