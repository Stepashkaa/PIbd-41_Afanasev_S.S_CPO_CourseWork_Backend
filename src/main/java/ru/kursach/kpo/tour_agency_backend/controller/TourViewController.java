package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.service.entity.TourViewService;

@RestController
@RequestMapping("/api/v1/tour-views")
@RequiredArgsConstructor
@Tag(name = "Просмотры туров")
@SecurityRequirement(name = "bearerAuth")
public class TourViewController {

    private final TourViewService tourViewService;

    @PostMapping("/{tourId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addView(@PathVariable Long tourId) {
        tourViewService.addView(tourId);
    }
}