package ru.kursach.kpo.tour_agency_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;

@Builder
@Schema(description = "Пользователь (ответ)")
public record UserResponseDto(

        @Schema(description = "ID пользователя", example = "1")
        Long id,

        @Schema(description = "Имя пользователя", example = "Jon")
        String username,

        @Schema(description = "Email", example = "jon@example.com")
        String email,

        @Schema(description = "Телефон", example = "+7 900 123-45-67")
        String phone,

        @Schema(description = "Роль пользователя", example = "MANAGER")
        UserRole userRole,

        @Schema(description = "Активен ли пользователь", example = "true")
        boolean active
) {}