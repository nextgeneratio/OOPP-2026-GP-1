package lk.ac.ruhuna.fot.ams.api.response;

public record CalculateGradeResponse(
        int offeringId,
        String gradeLetter,
        double gradePoint
) {}
