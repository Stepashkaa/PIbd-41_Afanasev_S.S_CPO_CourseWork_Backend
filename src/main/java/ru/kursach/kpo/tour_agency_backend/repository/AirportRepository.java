package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.AirportEntity;

import java.util.Optional;

@Repository
public interface AirportRepository extends JpaRepository<AirportEntity, Long> {

    boolean existsByIataCodeIgnoreCase(String iataCode);

    Optional<AirportEntity> findByIataCodeIgnoreCase(String iataCode);

    @Query("""
       SELECT a
       FROM AirportEntity a
       WHERE LOWER(a.iataCode) LIKE CONCAT('%', :iata, '%')
         AND LOWER(a.name) LIKE CONCAT('%', :name, '%')
         AND (:cityId IS NULL OR a.city.id = :cityId)
   """)
    Page<AirportEntity> search(
            @Param("iata") String iata,
            @Param("name") String name,
            @Param("cityId") Long cityId,
            Pageable pageable
    );
}
