package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourDepartureEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourDepartureStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TourDepartureRepository extends JpaRepository<TourDepartureEntity, Long>, JpaSpecificationExecutor<TourDepartureEntity> {

    List<TourDepartureEntity> findByTour_Id(Long tourId);

    List<TourDepartureEntity> findByStatus(TourDepartureStatus status);

    List<TourDepartureEntity> findByStartDateBetween(LocalDate from, LocalDate to);

    @Query("""
           SELECT d
           FROM TourDepartureEntity d
           WHERE (:tourId IS NULL OR d.tour.id = :tourId)
             AND (:status IS NULL OR d.status = :status)
             AND (:startFrom IS NULL OR d.startDate >= :startFrom)
             AND (:startTo IS NULL OR d.startDate <= :startTo)
           """)
    Page<TourDepartureEntity> searchPaged(
            @Param("tourId") Long tourId,
            @Param("status") TourDepartureStatus status,
            @Param("startFrom") LocalDate startFrom,
            @Param("startTo") LocalDate startTo,
            org.springframework.data.domain.Pageable pageable
    );
}