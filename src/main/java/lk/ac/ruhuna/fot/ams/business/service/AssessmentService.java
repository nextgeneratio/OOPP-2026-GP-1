package lk.ac.ruhuna.fot.ams.business.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator;
import lk.ac.ruhuna.fot.ams.domain.model.Assessment;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class AssessmentService {

    public interface AssessmentLookup {
        List<Assessment> findAssessmentsByOffering(int offeringId);
        boolean isAssessmentPlanPublished(int offeringId);
    }

    private final AssessmentLookup assessmentLookup;

    public AssessmentService(AssessmentLookup assessmentLookup) {
        this.assessmentLookup = assessmentLookup;
    }

    /**
     * Validates that all assessments for a course offering sum up exactly to 100% (BR-011).
     */
    public void validateAssessmentPlan(int offeringId) {
        List<Assessment> assessments = assessmentLookup.findAssessmentsByOffering(offeringId);
        if (assessments == null || assessments.isEmpty()) {
            throw new BusinessRuleException("No assessments configured for course offering " + offeringId);
        }
        double totalWeight = assessments.stream().mapToDouble(Assessment::weightPercent).sum();
        if (Math.abs(totalWeight - 100.0) > 0.01) {
            throw new BusinessRuleException("Assessment weights do not sum to 100%. Current total: " + totalWeight + "%");
        }
    }

    /**
     * Calculates final grade and marks for a student given their assessment scores.
     * Prevents calculation if assessment configuration is incomplete (doesn't sum to 100% or missing scores).
     */
    public GradeScheme calculateStudentFinalGrade(int offeringId, Map<String, Double> studentScoresByName) {
        validateAssessmentPlan(offeringId);

        List<Assessment> assessments = assessmentLookup.findAssessmentsByOffering(offeringId);
        if (studentScoresByName == null) {
            throw new ValidationException("Scores map cannot be null.");
        }

        // Verify all configured assessments have scores entered
        for (Assessment assessment : assessments) {
            if (!studentScoresByName.containsKey(assessment.name())) {
                throw new BusinessRuleException(
                        "Incomplete assessment score data: Missing mark for mandatory component '" + assessment.name() + "'"
                );
            }
        }

        List<AcademicCalculator.AssessmentItem> items = assessments.stream()
                .map(a -> new AcademicCalculator.AssessmentItem(studentScoresByName.get(a.name()), a.weightPercent()))
                .collect(Collectors.toList());

        double finalMark = AcademicCalculator.calculateFinalMark(items);
        return GradeScheme.fromMark(finalMark);
    }
}
