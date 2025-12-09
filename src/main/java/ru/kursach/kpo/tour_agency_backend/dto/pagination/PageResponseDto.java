package ru.kursach.kpo.tour_agency_backend.dto.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Страница результатов с пагинацией")
public record PageResponseDto<T>(

        @Schema(description = "Текущий номер страницы (0-based)", example = "0")
        int page,

        @Schema(description = "Размер страницы (количество элементов)", example = "10")
        int size,

        @Schema(description = "Общее количество страниц", example = "5")
        int totalPages,

        @Schema(description = "Общее количество элементов", example = "42")
        long totalElements,

        @Schema(description = "Список элементов на текущей странице")
        List<T> content
) {}
