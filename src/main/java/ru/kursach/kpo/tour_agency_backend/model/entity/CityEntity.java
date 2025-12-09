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
@Entity
@Builder
@Table(
        name = "cities",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name", "country"})
        }
)
@ToString
public class CityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 150)
    @Column(name = "country", nullable = false, length = 150)
    private String country;

    @Size(max = 50)
    @Column(name = "timezone", length = 50)
    private String timezone;

    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<AirportEntity> airports = new ArrayList<>();

    @OneToMany(mappedBy = "baseCity", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<TourEntity> tours = new ArrayList<>();

    public void addAirport(AirportEntity airport) {
        airports.add(airport);
        airport.setCity(this);
    }

    public void removeAirport(AirportEntity airport) {
        airports.remove(airport);
        airport.setCity(null);
    }

    public void addTour(TourEntity tour) {
        tours.add(tour);
        tour.setBaseCity(this);
    }

    public void removeTour(TourEntity tour) {
        tours.remove(tour);
        tour.setBaseCity(null);
    }
}
