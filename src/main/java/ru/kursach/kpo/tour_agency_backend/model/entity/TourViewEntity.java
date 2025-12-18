package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "tour_views",
        indexes = {
                @Index(name = "idx_tv_user_viewed", columnList = "user_id, viewed_at"),
                @Index(name = "idx_tv_tour", columnList = "tour_id")
        })
public class TourViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="user_id", nullable = false)
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="tour_id", nullable = false)
    @ToString.Exclude
    private TourEntity tour;

    @PrePersist
    public void prePersist() {
        if (viewedAt == null) viewedAt = LocalDateTime.now();
    }
}
