package ru.kursach.kpo.tour_agency_backend.mapper;

import org.springframework.stereotype.Component;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;

@Component
public class UserMapper {

    public UserEntity toEntity(UserCreateRequest request, String encodedPassword) {
        return UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(encodedPassword)
                .phone(request.phone())
                .userRole(request.userRole())
                .active(request.active() == null || request.active())
                .build();
    }

    public void updateEntity(UserUpdateRequest request,
                             String encodedPasswordOrNull,
                             UserEntity entity) {
        entity.setUsername(request.username());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setUserRole(request.userRole());
        entity.setActive(Boolean.TRUE.equals(request.active()));

        if (encodedPasswordOrNull != null) {
            entity.setPassword(encodedPasswordOrNull);
        }
    }

    public UserResponseDto toDto(UserEntity entity) {
        return UserResponseDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .userRole(entity.getUserRole())
                .active(entity.isActive())
                .build();
    }
}
