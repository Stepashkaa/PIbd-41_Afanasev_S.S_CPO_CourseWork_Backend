package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;

import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<AirportEntity, Long> {

    boolean existsByIataCodeIgnoreCase(String iataCode);

    Optional<AirportEntity> findByIataCodeIgnoreCase(String iataCode);

    Page<AirportEntity> findByIataCodeContainingIgnoreCaseAndNameContainingIgnoreCaseAndCity_NameContainingIgnoreCase(
            String iataCode,
            String name,
            String cityName,
            Pageable pageable
    );
}
