package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "tours")
public class TourEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @NotNull
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TourStatus status = TourStatus.DRAFT;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_city_id", nullable = false)
    @ToString.Exclude
    private CityEntity baseCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id")
    @ToString.Exclude
    private UserEntity managerUser;

    @OneToMany(mappedBy = "tour", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    @ToString.Exclude
    private List<TourDepartureEntity> departures = new ArrayList<>();

    public void addDeparture(TourDepartureEntity departure) {
        departures.add(departure);
        departure.setTour(this);
    }

    public void removeDeparture(TourDepartureEntity departure) {
        departures.remove(departure);
        departure.setTour(null);
    }
}
