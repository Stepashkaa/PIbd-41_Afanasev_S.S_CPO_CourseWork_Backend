package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourViewEntity;

import java.util.List;

public interface TourViewRepository extends JpaRepository<TourViewEntity, Long> {
    List<TourViewEntity> findTop20ByUser_IdOrderByViewedAtDesc(Long userId);
}
