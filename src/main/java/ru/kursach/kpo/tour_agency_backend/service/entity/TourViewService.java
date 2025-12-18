package ru.kursach.kpo.tour_agency_backend.service.entity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourViewEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourViewRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class TourViewService {

    private final TourViewRepository tourViewRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addView(Long tourId) {
        UserEntity user = currentUser();
        TourEntity tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Тур не найден"));

        tourViewRepository.save(TourViewEntity.builder()
                .user(user)
                .tour(tour)
                .build());
    }

    private UserEntity currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
    }
}
