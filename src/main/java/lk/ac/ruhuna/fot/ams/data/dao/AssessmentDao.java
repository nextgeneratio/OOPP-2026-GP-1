package lk.ac.ruhuna.fot.ams.data.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public interface AssessmentDao {
    record AssessmentRow(
            int id, int offeringId, String name, String type, BigDecimal weight, boolean caComponent) {
    }

    record MarkRow(
            int id, int assessmentId, int enrollmentId, BigDecimal score, int enteredBy) {
    }

    long insertAssessment(Connection connection, AssessmentRow row);

    long insertMark(Connection connection, MarkRow row);

    int reviseMark(Connection connection, MarkRow row);

    List<AssessmentRow> findAssessments(int offeringId);

    List<MarkRow> findMarks(int offeringId, Integer enrollmentId);

    List<MarkRow> findMarks(Connection connection, int offeringId, int enrollmentId);
}
