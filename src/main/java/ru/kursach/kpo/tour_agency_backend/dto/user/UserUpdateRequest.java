package ru.kursach.kpo.tour_agency_backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;

@Schema(description = "Запрос на обновление пользователя (админ)")
public record UserUpdateRequest(

        @Schema(description = "Имя пользователя", example = "Jon")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        @NotBlank(message = "Имя пользователя не может быть пустым")
        String username,

        @Schema(description = "Email", example = "jon@example.com")
        @Size(min = 5, max = 255, message = "Email должен содержать от 5 до 255 символов")
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email,

        @Schema(description = "Новый пароль (опционально)", example = "my_1secret1_password")
        @Size(min = 8, max = 255, message = "Пароль должен содержать от 8 до 255 символов")
        String password,   // может быть null / пустым — тогда не меняем

        @Schema(description = "Телефон", example = "+7 900 123-45-67")
        @Pattern(regexp = "^[+0-9()\\-\\s]+$", message = "Телефон содержит недопустимые символы")
        @Size(max = 50, message = "Телефон не должен превышать 50 символов")
        String phone,

        @Schema(description = "Роль пользователя", example = "MANAGER")
        @NotNull(message = "Роль пользователя обязательна")
        UserRole userRole,

        @Schema(description = "Активен ли пользователь", example = "true")
        @NotNull(message = "Признак активности обязателен")
        Boolean active
) {}