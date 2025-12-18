package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.RecommendationEntity;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<RecommendationEntity, Long> {

    @Query("""
      select r
      from RecommendationEntity r
      join r.tourDeparture d
      join d.tour t
      where r.id in :ids
    """)
    List<RecommendationEntity> findAllByIdWithTour(@Param("ids") List<Long> ids);

    Page<RecommendationEntity> findByUserSearch_IdOrderByScoreDescCreatedAtDesc(Long userSearchId, Pageable pageable);

    long countByUserSearch_Id(Long userSearchId);

    @Modifying
    @Query("""
    update RecommendationEntity r
    set r.selected = true
    where r.id = :id
      and r.userSearch.user.id = :userId
""")
    int markSelectedOwned(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("""
    update RecommendationEntity r
    set r.shown = true
    where r.id in :ids
      and r.userSearch.id = :searchId
      and r.shown = false
""")
    int markShown(@Param("searchId") Long searchId, @Param("ids") List<Long> ids);
}