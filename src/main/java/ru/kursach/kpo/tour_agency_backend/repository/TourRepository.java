package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.TourStatus;

import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<TourEntity, Long> {

    @Query("""
       SELECT t
       FROM TourEntity t
       WHERE (:title IS NULL OR :title = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%')))
         AND (:cityId IS NULL OR t.baseCity.id = :cityId)
         AND (:status IS NULL OR t.status = :status)
         AND (:active IS NULL OR t.active = :active)
         AND (:managerId IS NULL OR t.managerUser.id = :managerId)
       """)
    Page<TourEntity> searchPaged(
            @Param("title") String title,
            @Param("cityId") Long cityId,
            @Param("status") TourStatus status,
            @Param("active") Boolean active,
            @Param("managerId") Long managerId,
            Pageable pageable
    );
}