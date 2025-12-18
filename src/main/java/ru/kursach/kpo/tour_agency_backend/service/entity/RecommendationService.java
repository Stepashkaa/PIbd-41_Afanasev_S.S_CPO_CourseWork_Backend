package ru.kursach.kpo.tour_agency_backend.service.entity;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import ru.kursach.kpo.tour_agency_backend.dto.ml.MlRecoItem;
import ru.kursach.kpo.tour_agency_backend.dto.ml.MlRecoResponse;
import ru.kursach.kpo.tour_agency_backend.dto.pagination.PageResponseDto;
import ru.kursach.kpo.tour_agency_backend.dto.recommendation.RecommendedTourCardDto;
import ru.kursach.kpo.tour_agency_backend.model.entity.*;
import ru.kursach.kpo.tour_agency_backend.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final UserSearchRepository userSearchRepository;
    private final BookingRepository bookingRepository;
    private final TourViewRepository tourViewRepository;
    private final TourDepartureRepository tourDepartureRepository;
    private final UserRepository userRepository;

    private final RestClient recommendationEngineClient;


    @Transactional
    public PageResponseDto<RecommendedTourCardDto> getMyRecommendations(Long userSearchId, int page, int size) {
        if (userSearchId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "searchId обязателен");
        }

        Long userId = currentUserId();

        UserSearchEntity search = userSearchRepository.findById(userSearchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Поиск не найден"));

        if (!search.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваш searchId");
        }

        if (recommendationRepository.countByUserSearch_Id(userSearchId) == 0) {
            generateForSearch(search);
        }

        var pageable = PageRequest.of(page, size,
                Sort.by("score").descending().and(Sort.by("createdAt").descending()));

        var recPage = recommendationRepository
                .findByUserSearch_IdOrderByScoreDescCreatedAtDesc(userSearchId, pageable);

        List<Long> shownIds = recPage.getContent().stream()
                .map(RecommendationEntity::getId)
                .toList();
        if (!shownIds.isEmpty()) {
            recommendationRepository.markShown(userSearchId, shownIds);
        }

        var content = recPage.getContent().stream().map(this::toCardDto).toList();

        return PageResponseDto.<RecommendedTourCardDto>builder()
                .page(recPage.getNumber())
                .size(recPage.getSize())
                .totalPages(recPage.getTotalPages())
                .totalElements(recPage.getTotalElements())
                .content(content)
                .build();
    }

    @Transactional
    public void markSelected(Long recommendationId) {
        Long userId = currentUserId();
        int updated = recommendationRepository.markSelectedOwned(recommendationId, userId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Рекомендация не найдена");
            // (или FORBIDDEN, но чтобы не светить чужие id — часто оставляют NOT_FOUND)
        }
    }

    public MlRecoResponse callEngine(Long searchId) {
        return recommendationEngineClient.post()
                .uri("/recommendations/generate")
                .body(Map.of("searchId", searchId, "limit", 100))
                .retrieve()
                .body(MlRecoResponse.class);
    }

    @Transactional
    private void generateForSearch(UserSearchEntity search) {
        // ВАЖНО: путь должен совпадать с твоим recommendation_engine
        // например: "/recommendations/generate"
        MlRecoResponse ml = callEngine(search.getId());

        if (ml == null || ml.items() == null || ml.items().isEmpty()) return;

        List<Long> depIds = ml.items().stream().map(MlRecoItem::tourDepartureId).toList();

        Map<Long, TourDepartureEntity> depMap = tourDepartureRepository.findAllById(depIds)
                .stream()
                .collect(Collectors.toMap(TourDepartureEntity::getId, d -> d));

        List<RecommendationEntity> recs = new ArrayList<>();
        for (MlRecoItem item : ml.items()) {
            TourDepartureEntity dep = depMap.get(item.tourDepartureId());
            if (dep == null) continue;

            if (!matchesSearchFilters(dep, search)) continue;

            recs.add(RecommendationEntity.builder()
                    .userSearch(search)
                    .tourDeparture(dep)
                    .score(BigDecimal.valueOf(item.score()).setScale(4, RoundingMode.HALF_UP))
                    .shown(false)
                    .selected(false)
                    .build());
        }

        recommendationRepository.saveAll(recs);
    }

    private boolean matchesSearchFilters(TourDepartureEntity d, UserSearchEntity s) {
        TourEntity t = d.getTour();
        if (t == null) return false;

        // город назначения (если задан) — сравниваем с baseCity тура (можешь потом поменять логику)
        if (s.getDestinationCity() != null) {
            Long destCityId = s.getDestinationCity().getId();
            boolean ok = tourDepartureRepository.existsByIdAndArrivalCity(d.getId(), destCityId);
            if (!ok) return false;
        }

        // даты: пусть startDate попадает в диапазон
        if (s.getDateFrom() != null && d.getStartDate().isBefore(s.getDateFrom())) return false;
        if (s.getDateTo() != null && d.getStartDate().isAfter(s.getDateTo())) return false;

        // люди: должны быть места
        int persons = s.getPersonsCount() != null ? s.getPersonsCount() : 1;
        int available = (d.getCapacityTotal() != null ? d.getCapacityTotal() : 0)
                - (d.getCapacityReserved() != null ? d.getCapacityReserved() : 0);
        if (available < persons) return false;

        // бюджет: считаем ОБЩИЙ бюджет на поездку = pricePerPerson * personsCount
        BigDecimal pricePerPerson = d.getPriceOverride() != null ? d.getPriceOverride() : t.getBasePrice();
        if (pricePerPerson == null) return false;

        BigDecimal total = pricePerPerson.multiply(BigDecimal.valueOf(persons));

        if (s.getBudgetMin() != null && total.compareTo(s.getBudgetMin()) < 0) return false;
        if (s.getBudgetMax() != null && total.compareTo(s.getBudgetMax()) > 0) return false;

        return true;
    }

    private RecommendedTourCardDto toCardDto(RecommendationEntity rec) {
        var d = rec.getTourDeparture();
        var t = d.getTour();
        BigDecimal pricePerPerson = d.getPriceOverride() != null ? d.getPriceOverride() : t.getBasePrice();

        return RecommendedTourCardDto.builder()
                .recommendationId(rec.getId())
                .score(rec.getScore() != null ? rec.getScore().doubleValue() : null) // BigDecimal -> Double ✅
                .tourId(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .durationDays(t.getDurationDays())
                .baseCityId(t.getBaseCity().getId())
                .baseCityName(t.getBaseCity().getName())
                .basePrice(t.getBasePrice())
                .tourDepartureId(d.getId())
                .startDate(d.getStartDate())
                .endDate(d.getEndDate())
                .capacityTotal(d.getCapacityTotal())
                .capacityReserved(d.getCapacityReserved())
                .priceOverride(d.getPriceOverride())
                .pricePerPerson(pricePerPerson)
                .build();
    }

    private BigDecimal calcScore(List<BookingEntity> bookings,
                                 List<TourViewEntity> views,
                                 UserSearchEntity search,
                                 TourDepartureEntity cand) {

        // базовый скор (если вообще нет сигналов)
        double base = baseScoreNoHistory(cand).doubleValue();

        // сигналы предпочтения по городу: bookings > views
        Map<Long, Double> cityWeight = new HashMap<>();

        // bookings: вес 1.0
        if (bookings != null) {
            for (var b : bookings) {
                var d = b.getTourDeparture();
                if (d == null || d.getTour() == null || d.getTour().getBaseCity() == null) continue;
                Long cityId = d.getTour().getBaseCity().getId();
                cityWeight.put(cityId, cityWeight.getOrDefault(cityId, 0.0) + 1.0);
            }
        }

        // views: вес 0.35
        if (views != null) {
            for (var v : views) {
                var t = v.getTour();
                if (t == null || t.getBaseCity() == null) continue;
                Long cityId = t.getBaseCity().getId();
                cityWeight.put(cityId, cityWeight.getOrDefault(cityId, 0.0) + 0.35);
            }
        }

        Long candCityId = cand.getTour().getBaseCity().getId();
        double max = cityWeight.values().stream().max(Double::compareTo).orElse(1.0);
        double cityScore = cityWeight.getOrDefault(candCityId, 0.0) / max; // 0..1

        // бюджет ближе к верхней границе (если задан) — небольшой бонус (без жёсткой оптимизации)
        double budgetScore = 0.5;
        if (search.getBudgetMax() != null) {
            int persons = search.getPersonsCount() != null ? search.getPersonsCount() : 1;
            BigDecimal pp = cand.getPriceOverride() != null ? cand.getPriceOverride() : cand.getTour().getBasePrice();
            BigDecimal total = pp.multiply(BigDecimal.valueOf(persons));
            budgetScore = similarity(total.doubleValue(), search.getBudgetMax().doubleValue());
        }

        // скоро начинается — бонус
        long daysToStart = ChronoUnit.DAYS.between(LocalDate.now(), cand.getStartDate());
        double soonScore = 1.0 / (1.0 + Math.max(0, daysToStart) / 7.0);

        double total =
                0.50 * cityScore +
                        0.20 * budgetScore +
                        0.20 * soonScore +
                        0.10 * base;

        return BigDecimal.valueOf(total).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal baseScoreNoHistory(TourDepartureEntity cand) {
        long daysToStart = ChronoUnit.DAYS.between(LocalDate.now(), cand.getStartDate());
        double soonScore = 1.0 / (1.0 + Math.max(0, daysToStart) / 7.0);
        return BigDecimal.valueOf(0.3 + 0.7 * soonScore).setScale(4, RoundingMode.HALF_UP);
    }

    private double similarity(double x, double center) {
        double denom = Math.max(1.0, Math.abs(center));
        double diff = Math.abs(x - center) / denom;
        return 1.0 / (1.0 + diff);
    }

    private Long currentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"))
                .getId();
    }

    private record ScoredDeparture(TourDepartureEntity dep, BigDecimal score) {}
}