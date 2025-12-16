package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.kursach.kpo.tour_agency_backend.core.configuration.Constants;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserCreateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserUpdateRequest;
import ru.kursach.kpo.tour_agency_backend.dto.user.UserResponseDto;
import ru.kursach.kpo.tour_agency_backend.model.enums.UserRole;
import ru.kursach.kpo.tour_agency_backend.service.entity.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи (админ)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDto<UserResponseDto> getAllPaged(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "role", required = false) UserRole role,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return userService.getAllPaged(q, role, active, page, size);
    }

    @Operation(summary = "Поиск пользователей по имени (с пагинацией)")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponseDto<UserResponseDto> searchByUsername(
            @RequestParam(name = "username") String username,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = Constants.DEFAULT_PAGE_SIZE) int size
    ) {
        return userService.searchByUsername(username, page, size);
    }

    @Operation(summary = "Создать пользователя")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto create(@RequestBody @Valid UserCreateRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Получить пользователя по id")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Получить список всех пользователей")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDto> getAll() {
        return userService.getAll();
    }

    @Operation(summary = "Обновить пользователя")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto update(@PathVariable Long id,
                                  @RequestBody @Valid UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Удалить пользователя")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
