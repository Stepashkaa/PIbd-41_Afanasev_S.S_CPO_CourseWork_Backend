package ru.kursach.kpo.tour_agency_backend.service.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserResponseDto;
import ru.kursach.kpo.tour_agency_backend.mapper.UserMapper;
import ru.kursach.kpo.tour_agency_backend.model.entity.BookingEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.TourEntity;
import ru.kursach.kpo.tour_agency_backend.model.entity.UserEntity;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> getAllPaged(
            String username,
            String email,
            UserRole role,
            Boolean active,
            int page,
            int size
    ) {
        String usernameFilter = username != null && !username.isBlank()
                ? username.trim()
                : null;
        String emailFilter = email != null && !email.isBlank()
                ? email.trim()
                : null;

        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("username").ascending().and(Sort.by("id").ascending())
        );

        Page<UserEntity> userPage = userRepository.searchPaged(
                usernameFilter,
                emailFilter,
                role,
                active,
                pageable
        );

        return PageResponseDto.<UserResponseDto>builder()
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalPages(userPage.getTotalPages())
                .totalElements(userPage.getTotalElements())
                .content(userPage.getContent().stream()
                        .map(userMapper::toDto)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<UserResponseDto> searchByUsername(
            String username,
            int page,
            int size
    ) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Параметр username обязателен для поиска пользователя"
            );
        }

        return getAllPaged(username, null, null, null, page, size);
    }

    @Transactional
    public UserResponseDto create(UserCreateRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким email уже существует");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пользователь с таким именем уже существует");
        }

        UserEntity user = userMapper.toEntity(
                request,
                passwordEncoder.encode(request.password())
        );

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь с id=" + id + " не найден"));

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional
    public UserResponseDto update(Long id, UserUpdateRequest request) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь с id=" + id + " не найден"));

        if (userRepository.existsByEmail(request.email())
                && !user.getEmail().equalsIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email уже используется");
        }

        if (userRepository.existsByUsername(request.username())
                && !user.getUsername().equalsIgnoreCase(request.username())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username уже используется");
        }

        String encodedPasswordOrNull = null;
        if (request.password() != null && !request.password().isBlank()) {
            encodedPasswordOrNull = passwordEncoder.encode(request.password());
        }

        UserRole oldRole = user.getUserRole();
        UserRole newRole = request.userRole();

        // Если пользователь был MANAGER, а стал USER/ADMIN → снимаем управление турами
        if (oldRole == UserRole.MANAGER && newRole != UserRole.MANAGER) {
            List<TourEntity> toursToRemove = List.copyOf(user.getManagedTours());
            for (TourEntity tour : toursToRemove) {
                user.removeManagedTour(tour);
            }
        }

        userMapper.updateEntity(request, encodedPasswordOrNull, user);

        userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Пользователь с id=" + id + " не найден"));

        for (TourEntity tour : List.copyOf(user.getManagedTours())) {
            user.removeManagedTour(tour);
        }

        try {
            userRepository.delete(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Невозможно удалить пользователя: к нему привязаны сущности"
            );
        }
    }
}
