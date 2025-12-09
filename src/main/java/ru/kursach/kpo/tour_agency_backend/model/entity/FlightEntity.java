package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.FlightStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(
        name = "flights",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"flight_number"})
        }
)
public class FlightEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "flight_number", nullable = false, length = 20)
    private String flightNumber;

    @NotBlank
    @Size(max = 150)
    @Column(name = "carrier", nullable = false, length = 150)
    private String carrier;

    @NotNull
    @Column(name = "depart_at", nullable = false)
    private LocalDateTime departAt;

    @NotNull
    @Column(name = "arrive_at", nullable = false)
    private LocalDateTime arriveAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    @NotNull
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_airport_id", nullable = false)
    @ToString.Exclude
    private AirportEntity departureAirport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrival_airport_id", nullable = false)
    @ToString.Exclude
    private AirportEntity arrivalAirport;

    @ManyToMany
    @JoinTable(
            name = "flight_tour_departure",
            joinColumns = @JoinColumn(name = "flight_id"),
            inverseJoinColumns = @JoinColumn(name = "tour_departure_id")
    )
    @Builder.Default
    @ToString.Exclude
    private List<TourDepartureEntity> tourDepartures = new ArrayList<>();

    @OneToMany(mappedBy = "selectedFlight", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<BookingEntity> bookings = new ArrayList<>();

    public void addTourDeparture(TourDepartureEntity tourDeparture) {
        tourDepartures.add(tourDeparture);
        tourDeparture.getFlights().add(this);
    }

    public void removeTourDeparture(TourDepartureEntity tourDeparture) {
        tourDepartures.remove(tourDeparture);
        tourDeparture.getFlights().remove(this);
    }

    public void addBooking(BookingEntity booking) {
        bookings.add(booking);
        booking.setSelectedFlight(this);
    }

    public void removeBooking(BookingEntity booking) {
        bookings.remove(booking);
        booking.setSelectedFlight(null);
    }
}
