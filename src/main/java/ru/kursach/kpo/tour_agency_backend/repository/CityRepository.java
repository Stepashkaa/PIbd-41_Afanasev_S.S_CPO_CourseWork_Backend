package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.CityEntity;

import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<CityEntity, Long> {

    boolean existsByNameAndCountryIgnoreCase(String name, String country);

    Optional<CityEntity> findByNameIgnoreCaseAndCountryIgnoreCase(String name, String country);

    Page<CityEntity> findByNameContainingIgnoreCaseAndCountryContainingIgnoreCase(
            String name,
            String country,
            Pageable pageable
    );

    @Query("""
           SELECT c
           FROM CityEntity c
           WHERE (:q IS NULL
               OR LOWER(c.name)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(c.country) LIKE LOWER(CONCAT('%', :q, '%'))
           )
           """)
    Page<CityEntity> searchFree(@Param("q") String q, Pageable pageable);
}