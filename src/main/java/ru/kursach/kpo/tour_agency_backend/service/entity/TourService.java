package ru.kursach.kpo.tour_agency_backend.service.entity;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.tour.TourUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.mapper.TourMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;
import ru.kursach.kpo.tour_agency_backend.repository.TourRepository;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final TourMapper tourMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<TourResponseDto> getAllPaged(
            String title,
            Long baseCityId,
            TourStatus status,
            Boolean active,
            Long managerUserId,
            int page,
            int size
    ) {
        String titleFilter = title != null ? title.trim() : null;

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("title").ascending()
        );

        Page<TourEntity> tours = tourRepository.searchPaged(
                titleFilter,
                baseCityId,
                status,
                active,
                managerUserId,
                pageable
        );

        return PageResponseDto.<TourResponseDto>builder()
                .page(tours.getNumber())
                .size(tours.getSize())
                .totalPages(tours.getTotalPages())
                .totalElements(tours.getTotalElements())
                .content(tours.getContent().stream()
                        .map(tourMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<TourResponseDto> searchByTitle(
            String title,
            int page,
            int size
    ) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр title обязателен для поиска тура"
            );
        }

        return getAllPaged(title, null, null, null, null, page, size);
    }


    @Transactional
    public TourResponseDto create(TourCreateRequest request) {
        CityEntity city = cityRepository.findById(request.baseCityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Город с id=" + request.baseCityId() + " не найден"
                ));

        UserEntity manager = resolveAndValidateManager(request.managerUserId());

        TourEntity tour = tourMapper.toEntity(request, city, manager);

        city.addTour(tour);
        if (manager != null) {
            manager.addManagedTour(tour);
        }

        tour = tourRepository.save(tour);
        return tourMapper.toDto(tour);
    }

    @Transactional(readOnly = true)
    public TourResponseDto getById(Long id) {
        TourEntity tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тур с id=" + id + " не найден"
                ));
        return tourMapper.toDto(tour);
    }

    @Transactional(readOnly = true)
    public List<TourResponseDto> getAll() {
        return tourRepository.findAll(Sort.by("title").ascending()).stream()
                .map(tourMapper::toDto)
                .toList();
    }

    @Transactional
    public TourResponseDto update(Long id, TourUpdateRequest request) {
        TourEntity tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тур с id=" + id + " не найден"
                ));

        CityEntity newCity = cityRepository.findById(request.baseCityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Город с id=" + request.baseCityId() + " не найден"
                ));

        UserEntity newManager = resolveAndValidateManager(request.managerUserId());

        boolean active = Boolean.TRUE.equals(request.active());
        validateStatusAndActive(request.status(), active);

        CityEntity oldCity = tour.getBaseCity();
        if (!oldCity.getId().equals(newCity.getId())) {
            oldCity.removeTour(tour);
            newCity.addTour(tour);
        }

        UserEntity oldManager = tour.getManagerUser();
        if (oldManager != null && (newManager == null || !oldManager.getId().equals(newManager.getId()))) {
            oldManager.removeManagedTour(tour);
        }
        if (newManager != null && (oldManager == null || !oldManager.getId().equals(newManager.getId()))) {
            newManager.addManagedTour(tour);
        }

        tourMapper.updateEntity(request, newCity, newManager, tour);
        tour = tourRepository.save(tour);

        return tourMapper.toDto(tour);
    }

    @Transactional
    public void delete(Long id) {
        TourEntity tour = tourRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Тур с id=" + id + " не найден"
                ));

        CityEntity city = tour.getBaseCity();
        UserEntity manager = tour.getManagerUser();

        if (city != null) {
            city.removeTour(tour);
        }
        if (manager != null) {
            manager.removeManagedTour(tour);
        }

        try {
            tourRepository.delete(tour);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить тур: к нему привязаны вылеты или бронирования"
            );
        }
    }

    private UserEntity resolveAndValidateManager(Long managerUserId) {
        if (managerUserId == null) {
            return null;
        }

        UserEntity manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Пользователь-менеджер с id=" + managerUserId + " не найден"
                ));

        if (manager.getUserRole() != UserRole.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пользователь с id=" + managerUserId + " не является менеджером"
            );
        }

        if (!manager.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пользователь с id=" + managerUserId + " деактивирован и не может быть менеджером тура"
            );
        }

        return manager;
    }

    private void validateStatusAndActive(TourStatus status, boolean active) {
        if (status == TourStatus.ARCHIVED && active) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Архивный тур не может быть активным"
            );
        }
    }
}