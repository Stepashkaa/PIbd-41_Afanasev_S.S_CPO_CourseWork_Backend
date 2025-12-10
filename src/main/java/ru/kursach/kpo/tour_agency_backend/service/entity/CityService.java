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
import ru.kursach.kpo.tour_agency_backend.dto.city.CityCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.city.CityUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.mapper.CityMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<CityResponseDto> getAllPaged(
            String nameFilter,
            String countryFilter,
            int page,
            int size
    ) {
        String name = nameFilter != null ? nameFilter.trim() : "";
        String country = countryFilter != null ? countryFilter.trim() : "";

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("name").ascending().and(Sort.by("country").ascending())
        );

        Page<CityEntity> cityPage = cityRepository
                .findByNameContainingIgnoreCaseAndCountryContainingIgnoreCase(
                        name,
                        country,
                        pageable
                );

        return PageResponseDto.<CityResponseDto>builder()
                .page(cityPage.getNumber())
                .size(cityPage.getSize())
                .totalPages(cityPage.getTotalPages())
                .totalElements(cityPage.getTotalElements())
                .content(cityPage.getContent().stream()
                        .map(cityMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public CityResponseDto findByNameAndCountry(String name, String country) {
        if (name == null || name.isBlank() || country == null || country.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметры name и country обязательны для поиска города"
            );
        }

        CityEntity city = cityRepository
                .findByNameIgnoreCaseAndCountryIgnoreCase(name.trim(), country.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Город с названием '" + name + "' и страной '" + country + "' не найден"
                ));

        return cityMapper.toDto(city);
    }

    @Transactional(readOnly = true)
    public PageResponseDto<CityResponseDto> searchFree(
            String query,
            int page,
            int size
    ) {
        String q = (query != null && !query.trim().isBlank())
                ? query.trim()
                : null;

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("name").ascending().and(Sort.by("country").ascending())
        );

        Page<CityEntity> cityPage = cityRepository.searchFree(q, pageable);

        return PageResponseDto.<CityResponseDto>builder()
                .page(cityPage.getNumber())
                .size(cityPage.getSize())
                .totalPages(cityPage.getTotalPages())
                .totalElements(cityPage.getTotalElements())
                .content(cityPage.getContent().stream()
                        .map(cityMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional
    public CityResponseDto create(CityCreateRequest request) {
        if (cityRepository.existsByNameAndCountryIgnoreCase(request.name(), request.country())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Город с таким названием и страной уже существует"
            );
        }

        CityEntity city = cityMapper.toEntity(request);
        city = cityRepository.save(city);
        return cityMapper.toDto(city);
    }

    @Transactional(readOnly = true)
    public CityResponseDto getById(Long id) {
        CityEntity city = cityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Город с id=" + id + " не найден"
                ));
        return cityMapper.toDto(city);
    }

    @Transactional(readOnly = true)
    public List<CityResponseDto> getAll() {
        return cityRepository.findAll().stream()
                .map(cityMapper::toDto)
                .toList();
    }

    @Transactional
    public CityResponseDto update(Long id, CityUpdateRequest request) {
        CityEntity city = cityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Город с id=" + id + " не найден"
                ));

        boolean exists = cityRepository.existsByNameAndCountryIgnoreCase(
                request.name(), request.country()
        );
        if (exists && !(city.getName().equalsIgnoreCase(request.name())
                && city.getCountry().equalsIgnoreCase(request.country()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Город с таким названием и страной уже существует"
            );
        }

        cityMapper.updateEntity(request, city);
        city = cityRepository.save(city);

        return cityMapper.toDto(city);
    }

    @Transactional
    public void delete(Long id) {
        CityEntity city = cityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Город с id=" + id + " не найден"
                ));

        try {
            cityRepository.delete(city);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить город: на него ссылаются аэропорты или туры"
            );
        }
    }
}
