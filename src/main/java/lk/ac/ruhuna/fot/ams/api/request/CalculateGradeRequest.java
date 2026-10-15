package lk.ac.ruhuna.fot.ams.api.request;

import java.util.Map;

public record CalculateGradeRequest(
        Integer offeringId,
        Map<String, Double> studentScores
) {}
