package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.dto.recommendation.UserSearchCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.recommendation.UserSearchResponseDto;
import ru.kursach.kpo.tour_agency_backend.service.entity.UserSearchService;

@RestController
@RequestMapping("/api/v1/user-searches")
@RequiredArgsConstructor
@Tag(name = "Поиск пользователя (UserSearch)")
@SecurityRequirement(name = "bearerAuth")
public class UserSearchController {

    private final UserSearchService userSearchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserSearchResponseDto create(@RequestBody @Valid UserSearchCreateRequest request) {
        Long id = userSearchService.create(request);
        return new UserSearchResponseDto(id);
    }
}
