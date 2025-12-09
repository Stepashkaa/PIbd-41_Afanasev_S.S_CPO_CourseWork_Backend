package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "airports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"iata_code"})
        }
)
@ToString
public class AirportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 10)
    @Column(name = "iata_code", nullable = false, length = 10)
    private String iataCode;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private CityEntity city;

    @OneToMany(mappedBy = "departureAirport", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<FlightEntity> departureFlights = new ArrayList<>();

    @OneToMany(mappedBy = "arrivalAirport", fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<FlightEntity> arrivalFlights = new ArrayList<>();

    public void addDepartureFlight(FlightEntity flight) {
        departureFlights.add(flight);
        flight.setDepartureAirport(this);
    }

    public void removeDepartureFlight(FlightEntity flight) {
        departureFlights.remove(flight);
        flight.setDepartureAirport(null);
    }

    public void addArrivalFlight(FlightEntity flight) {
        arrivalFlights.add(flight);
        flight.setArrivalAirport(this);
    }

    public void removeArrivalFlight(FlightEntity flight) {
        arrivalFlights.remove(flight);
        flight.setArrivalAirport(null);
    }
}
