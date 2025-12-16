package ru.kursach.kpo.tour_agency_backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findByUserRole(UserRole userRole);

    @Query("""
       SELECT u
       FROM UserEntity u
       WHERE (:q = '' OR
              LOWER(u.username) LIKE CONCAT('%', :q, '%') OR
              LOWER(u.email) LIKE CONCAT('%', :q, '%') OR
              LOWER(COALESCE(u.phone, '')) LIKE CONCAT('%', :q, '%')
       )
       AND (:role IS NULL OR u.userRole = :role)
       AND (:active IS NULL OR u.active = :active)
       """)
    Page<UserEntity> searchPaged(
            @Param("q") String q,
            @Param("role") UserRole role,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
