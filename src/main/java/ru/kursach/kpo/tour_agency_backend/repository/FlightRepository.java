package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;

import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, Long> {

    boolean existsByFlightNumberIgnoreCase(String flightNumber);

    @Query("""
       SELECT f
       FROM FlightEntity f
       WHERE LOWER(f.flightNumber) LIKE CONCAT('%', :flightNumber, '%')
         AND LOWER(f.departureAirport.name) LIKE CONCAT('%', :departureAirportName, '%')
         AND LOWER(f.arrivalAirport.name) LIKE CONCAT('%', :arrivalAirportName, '%')
       """)
    Page<FlightEntity> search(
            @Param("flightNumber") String flightNumber,
            @Param("departureAirportName") String departureAirportName,
            @Param("arrivalAirportName") String arrivalAirportName,
            Pageable pageable
    );

    Optional<FlightEntity> findByFlightNumberIgnoreCase(String flightNumber);

    @Query("""
    SELECT f
    FROM FlightEntity f
    WHERE (:cityId IS NULL
           OR f.departureAirport.city.id = :cityId
           OR f.arrivalAirport.city.id = :cityId)
      AND (:flightNumber = '' 
           OR LOWER(f.flightNumber) LIKE CONCAT('%', :flightNumber, '%'))
    """)
    Page<FlightEntity> searchByCityAndNumber(
            @Param("cityId") Long cityId,
            @Param("flightNumber") String flightNumber,
            Pageable pageable
    );

    @Query("""
    SELECT f
    FROM FlightEntity f
    WHERE (:cityId IS NULL
           OR f.departureAirport.city.id = :cityId
           OR f.arrivalAirport.city.id = :cityId)
      AND (:flightNumber = ''
           OR LOWER(f.flightNumber) LIKE CONCAT('%', :flightNumber, '%'))
      AND (f.arriveAt >= :from)
      AND (f.departAt < :toExclusive)
    """)
    Page<FlightEntity> searchForTourDeparture(
            @Param("cityId") Long cityId,
            @Param("flightNumber") String flightNumber,
            @Param("from") java.time.LocalDateTime from,
            @Param("toExclusive") java.time.LocalDateTime toExclusive,
            Pageable pageable
    );
}
