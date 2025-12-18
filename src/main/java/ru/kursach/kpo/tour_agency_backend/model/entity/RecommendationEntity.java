package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reco_search_departure",
                        columnNames = {"user_search_id", "tour_departure_id"}
                )
        },
        indexes = {
                @Index(name = "idx_reco_user_search", columnList = "user_search_id"),
                @Index(name = "idx_reco_tour_departure", columnList = "tour_departure_id"),
                @Index(name = "idx_reco_created_at", columnList = "created_at")
        }
)
public class RecommendationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "score", nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_shown", nullable = false)
    private boolean shown = false;

    @Builder.Default
    @Column(name = "is_selected", nullable = false)
    private boolean selected = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_search_id", nullable = false)
    @ToString.Exclude
    private UserSearchEntity userSearch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tour_departure_id", nullable = false)
    @ToString.Exclude
    private TourDepartureEntity tourDeparture;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        // shown/selected по дефолту уже false
    }
}
