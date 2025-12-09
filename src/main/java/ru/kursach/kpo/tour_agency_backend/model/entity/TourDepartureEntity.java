package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "tour_departures")
public class TourDepartureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull
    @Column(name = "capacity_total", nullable = false)
    private Integer capacityTotal;

    @NotNull
    @Column(name = "capacity_reserved", nullable = false)
    @Builder.Default
    private Integer capacityReserved = 0;

    @Column(name = "price_override", precision = 10, scale = 2)
    private BigDecimal priceOverride;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TourDepartureStatus status = TourDepartureStatus.PLANNED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    @ToString.Exclude
    private TourEntity tour;

    @ManyToMany(mappedBy = "tourDepartures")
    @Builder.Default
    @ToString.Exclude
    private List<FlightEntity> flights = new ArrayList<>();

    @OneToMany(mappedBy = "tourDeparture", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<BookingEntity> bookings = new ArrayList<>();


    public void addFlight(FlightEntity flight) {
        flights.add(flight);
        flight.getTourDepartures().add(this);
    }

    public void removeFlight(FlightEntity flight) {
        flights.remove(flight);
        flight.getTourDepartures().remove(this);
    }

    public void addBooking(BookingEntity booking) {
        bookings.add(booking);
        booking.setTourDeparture(this);
    }

    public void removeBooking(BookingEntity booking) {
        bookings.remove(booking);
        booking.setTourDeparture(null);
    }
}
