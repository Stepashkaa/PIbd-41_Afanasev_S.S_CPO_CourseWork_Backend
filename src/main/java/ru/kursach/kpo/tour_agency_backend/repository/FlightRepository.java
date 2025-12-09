package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.FlightEntity;

import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<FlightEntity, Long> {

    boolean existsByFlightNumberIgnoreCase(String flightNumber);

    Page<FlightEntity> findByFlightNumberContainingIgnoreCaseAndDepartureAirport_NameContainingIgnoreCaseAndArrivalAirport_NameContainingIgnoreCase(
            String flightNumber,
            String departureAirportName,
            String arrivalAirportName,
            Pageable pageable
    );

    Optional<FlightEntity> findByFlightNumberIgnoreCase(String flightNumber);
}
