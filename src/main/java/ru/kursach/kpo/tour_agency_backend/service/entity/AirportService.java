package ru.kursach.kpo.tour_agency_backend.service.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.airport.AirportUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.mapper.AirportMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;
import ru.kursach.kpo.tour_agency_backend.repository.AirportRepository;
import ru.kursach.kpo.tour_agency_backend.repository.CityRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AirportService {

    private final AirportRepository airportRepository;
    private final CityRepository cityRepository;
    private final AirportMapper airportMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<AirportResponseDto> getAllPaged(
            String iataFilter,
            String nameFilter,
            String cityNameFilter,
            int page,
            int size
    ) {
        String iata = iataFilter != null ? iataFilter.trim() : "";
        String name = nameFilter != null ? nameFilter.trim() : "";
        String cityName = cityNameFilter != null ? cityNameFilter.trim() : "";

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by("iataCode").ascending().and(Sort.by("name").ascending())
        );

        Page<AirportEntity> airportPage =
                airportRepository.findByIataCodeContainingIgnoreCaseAndNameContainingIgnoreCaseAndCity_NameContainingIgnoreCase(
                        iata,
                        name,
                        cityName,
                        pageable
                );

        return PageResponseDto.<AirportResponseDto>builder()
                .page(airportPage.getNumber())
                .size(airportPage.getSize())
                .totalPages(airportPage.getTotalPages())
                .totalElements(airportPage.getTotalElements())
                .content(airportPage.getContent().stream()
                        .map(airportMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public AirportResponseDto findByIataCode(String iataCode) {
        if (iataCode == null || iataCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "IATA-код обязателен для поиска"
            );
        }

        AirportEntity airport = airportRepository.findByIataCodeIgnoreCase(iataCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Аэропорт с IATA-кодом '" + iataCode + "' не найден"
                ));

        return airportMapper.toDto(airport);
    }


    @Transactional
    public AirportResponseDto create(AirportCreateRequest request) {
        if (airportRepository.existsByIataCodeIgnoreCase(request.iataCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Аэропорт с таким IATA-кодом уже существует"
            );
        }

        CityEntity city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Город с id=" + request.cityId() + " не найден"
                ));

        AirportEntity airport = airportMapper.toEntity(request, city);

        city.addAirport(airport);

        airport = airportRepository.save(airport);
        return airportMapper.toDto(airport);
    }

    @Transactional(readOnly = true)
    public AirportResponseDto getById(Long id) {
        AirportEntity airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Аэропорт с id=" + id + " не найден"
                ));
        return airportMapper.toDto(airport);
    }

    @Transactional(readOnly = true)
    public List<AirportResponseDto> getAll() {
        return airportRepository.findAll().stream()
                .map(airportMapper::toDto)
                .toList();
    }

    @Transactional
    public AirportResponseDto update(Long id, AirportUpdateRequest request) {
        AirportEntity airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Аэропорт с id=" + id + " не найден"
                ));

        boolean iataExists = airportRepository.existsByIataCodeIgnoreCase(request.iataCode());
        if (iataExists && !airport.getIataCode().equalsIgnoreCase(request.iataCode())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Аэропорт с таким IATA-кодом уже существует"
            );
        }

        CityEntity newCity = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Город с id=" + request.cityId() + " не найден"
                ));

        CityEntity oldCity = airport.getCity();

        if (!oldCity.getId().equals(newCity.getId())) {
            oldCity.removeAirport(airport);
            newCity.addAirport(airport);
        }

        airportMapper.updateEntity(request, airport);
        airport = airportRepository.save(airport);

        return airportMapper.toDto(airport);
    }

    @Transactional
    public void delete(Long id) {
        AirportEntity airport = airportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Аэропорт с id=" + id + " не найден"
                ));
        CityEntity city = airport.getCity();
        if (city != null) {
            city.removeAirport(airport);
        }

        try {
            airportRepository.delete(airport);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить аэропорт: на него ссылаются рейсы"
            );
        }
    }
}