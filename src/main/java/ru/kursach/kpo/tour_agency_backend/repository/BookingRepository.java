package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.BookingEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.BookingStatus;

import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
    @Query("""
           SELECT b
           FROM BookingEntity b
           WHERE (:userId IS NULL OR b.user.id = :userId)
             AND (:tourDepartureId IS NULL OR b.tourDeparture.id = :tourDepartureId)
             AND (:status IS NULL OR b.status = :status)
             AND (:createdFrom IS NULL OR b.createdAt >= :createdFrom)
             AND (:createdTo IS NULL OR b.createdAt <= :createdTo)
             AND (:userEmail IS NULL OR LOWER(b.user.email) LIKE LOWER(CONCAT('%', :userEmail, '%')))
           """)
    Page<BookingEntity> searchPaged(
            @Param("userId") Long userId,
            @Param("tourDepartureId") Long tourDepartureId,
            @Param("status") BookingStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            @Param("userEmail") String userEmail,
            org.springframework.data.domain.Pageable pageable
    );
}
