package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        select b
        from BookingEntity b
        join b.user u
        where (:userId is null or u.id = :userId)
          and (:tourDepartureId is null or b.tourDeparture.id = :tourDepartureId)
          and (:status is null or b.status = :status)
          and b.createdAt >= coalesce(:createdFrom, b.createdAt)
          and b.createdAt <= coalesce(:createdTo, b.createdAt)
    """)
    Page<BookingEntity> searchPaged(
            @Param("userId") Long userId,
            @Param("tourDepartureId") Long tourDepartureId,
            @Param("status") BookingStatus status,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable
    );
}
