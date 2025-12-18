package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserSearchEntity;

public interface UserSearchRepository extends JpaRepository<UserSearchEntity, Long> {
    Page<UserSearchEntity> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
