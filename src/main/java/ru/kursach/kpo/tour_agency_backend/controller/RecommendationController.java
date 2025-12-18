package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.recommendation.RecommendedTourCardDto;
import ru.kursach.kpo.tour_agency_backend.service.entity.RecommendationService;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Рекомендации")
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/my/paged")
    public PageResponseDto<RecommendedTourCardDto> getMyPaged(
            @RequestParam Long searchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return recommendationService.getMyRecommendations(searchId, page, size);
    }

    @PatchMapping("/{id}/select")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markSelected(@PathVariable Long id) {
        recommendationService.markSelected(id);
    }
}
