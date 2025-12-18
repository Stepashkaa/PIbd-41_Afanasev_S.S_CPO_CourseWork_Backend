package ru.kursach.kpo.tour_agency_backend.dto.ml;

import java.util.List;

public record MlRecoResponse(Long searchId, List<MlRecoItem> items) {}
