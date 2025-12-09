package ru.kursach.kpo.tour_agency_backend.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Ответ c токеном доступа")
public record JwtAuthenticationResponse (
    @Schema(description = "Токен доступа", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTYyMjUwNj...")
    String token
) {}