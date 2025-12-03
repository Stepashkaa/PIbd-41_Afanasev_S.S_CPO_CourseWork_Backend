package ru.kursach.kpo.tour_agency_backend.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@SecurityRequirement(name = "bearerAuth")
public class TestController {

    // просто доступен любому АВТОРИЗОВАННОМУ пользователю
    @GetMapping("/me")
    public String me() {
        return "OK: you are authenticated";
    }

    // только для ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String admin() {
        return "OK: you are ADMIN";
    }
}
