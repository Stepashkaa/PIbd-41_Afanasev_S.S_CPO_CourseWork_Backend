package ru.kursach.kpo.tour_agency_backend.service.entity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.recommendation.UserSearchCreateRequest;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserSearchEntity;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserSearchRepository;


@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final UserSearchRepository userSearchRepository;
    private final UserRepository userRepository;
    private final CityRepository cityRepository;

    @Transactional
    public Long create(UserSearchCreateRequest req) {
        UserEntity user = currentUser();

        CityEntity destCity = null;
        if (req.destinationCityId() != null) {
            destCity = cityRepository.findById(req.destinationCityId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Город назначения не найден: id=" + req.destinationCityId()));
        }

        UserSearchEntity s = UserSearchEntity.builder()
                .user(user)
                .destinationCity(destCity)
                .dateFrom(req.dateFrom())
                .dateTo(req.dateTo())
                .personsCount(req.personsCount())
                .budgetMin(req.budgetMin())
                .budgetMax(req.budgetMax())
                .preferencesJson(req.preferencesJson())
                .build();

        if (req.dateFrom() != null && req.dateTo() != null && req.dateFrom().isAfter(req.dateTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateFrom не может быть позже dateTo");
        }

        return userSearchRepository.save(s).getId();
    }

    private UserEntity currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
    }
}