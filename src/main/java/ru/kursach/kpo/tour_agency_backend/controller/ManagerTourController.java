package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;
import ru.kursach.kpo.tour_agency_backend.service.entity.TourService;

@RestController
@RequestMapping("/api/v1/manager/tours")
@RequiredArgsConstructor
@Tag(name = "Туры менеджера")
@SecurityRequirement(name = "bearerAuth")
public class ManagerTourController {

    private final TourService tourService;
    private final UserRepository userRepository;

    @GetMapping("/paged")
    @PreAuthorize("hasRole('MANAGER')")
    public PageResponseDto<TourResponseDto> getMyTours(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long baseCityId,
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        UserEntity manager = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Пользователь не найден"
                ));

        return tourService.getAllPaged(
                title,
                baseCityId,
                status,
                active,
                manager.getId(),
                page,
                size
        );
    }
}

