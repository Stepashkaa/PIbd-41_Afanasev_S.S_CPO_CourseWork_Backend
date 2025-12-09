package ru.kursach.kpo.tour_agency_backend.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@AllArgsConstructor
@Entity
@Builder
@Table(name = "app_users",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "email")
        }
)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    @EqualsAndHashCode.Include
    private String email;

    @NotBlank
    @Size(min = 8, max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Size(max = 50)
    @Column(name = "phone", length = 50)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 20)
    private UserRole userRole;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "managerUser", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<TourEntity> managedTours = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<BookingEntity> bookings = new ArrayList<>();

    public void addManagedTour(TourEntity tour) {
        managedTours.add(tour);
        tour.setManagerUser(this);
    }

    public void removeManagedTour(TourEntity tour) {
        managedTours.remove(tour);
        tour.setManagerUser(null);
    }

    public void addBooking(BookingEntity booking) {
        bookings.add(booking);
        booking.setUser(this);
    }

    public void removeBooking(BookingEntity booking) {
        bookings.remove(booking);
        booking.setUser(null);
    }
}
