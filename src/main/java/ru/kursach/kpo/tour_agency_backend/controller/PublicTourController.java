package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.service.entity.TourService;

@RestController
@RequestMapping("/api/v1/public/tours")
@RequiredArgsConstructor
@Tag(name = "Публичные туры")
public class PublicTourController {

    private final TourService tourService;

    @GetMapping("/paged")
    public PageResponseDto<TourResponseDto> getPublicTours(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long baseCityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return tourService.getAllPaged(
                title,
                baseCityId,
                TourStatus.PUBLISHED,
                true,
                null,
                page,
                size
        );
    }
}
