package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;

public final class JdbcMedicalDao implements MedicalDao {
    private static final String SELECT_COLUMNS = "SELECT medical_id, student_id, offering_id, medical_date, "
            + "start_date, end_date, reason_details, evidence_reference, approval_status, affects_eligibility "
            + "FROM medical_records ";

    private final JdbcExecutor jdbc;

    public JdbcMedicalDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insert(Connection connection, MedicalRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO medical_records "
                        + "(student_id, offering_id, medical_date, start_date, end_date, reason_details, "
                        + "evidence_reference, approval_status, affects_eligibility) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.studentId());
                    if (row.offeringId() == null) {
                        statement.setNull(2, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(2, row.offeringId());
                    }
                    statement.setDate(3, Date.valueOf(row.medicalDate()));
                    statement.setDate(4, Date.valueOf(row.startDate()));
                    statement.setDate(5, Date.valueOf(row.endDate()));
                    statement.setString(6, row.reason());
                    statement.setString(7, row.evidenceReference());
                    statement.setString(8, row.status().name());
                    statement.setBoolean(9, row.affectsEligibility());
                });
    }

    @Override
    public int decide(Connection connection, int medicalId, ApprovalStatus status) {
        return jdbc.update(connection,
                "UPDATE medical_records SET approval_status = ?, affects_eligibility = ? "
                        + "WHERE medical_id = ? AND approval_status = 'PENDING'",
                statement -> {
                    statement.setString(1, status.name());
                    statement.setBoolean(2, status == ApprovalStatus.APPROVED);
                    statement.setInt(3, medicalId);
                });
    }

    @Override
    public Optional<MedicalRow> findById(int medicalId) {
        return jdbc.query(SELECT_COLUMNS + "WHERE medical_id = ?",
                statement -> statement.setInt(1, medicalId), this::map).stream().findFirst();
    }

    @Override
    public List<MedicalRow> findForStudent(int studentId, Integer offeringId) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE student_id = ? AND (? IS NULL OR offering_id = ? OR offering_id IS NULL) "
                        + "ORDER BY start_date, medical_id",
                statement -> {
                    statement.setInt(1, studentId);
                    if (offeringId == null) {
                        statement.setNull(2, java.sql.Types.INTEGER);
                        statement.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(2, offeringId);
                        statement.setInt(3, offeringId);
                    }
                }, this::map);
    }

    @Override
    public List<MedicalRow> findOverlapping(
            int studentId, Integer offeringId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE student_id = ? AND (? IS NULL OR offering_id = ? OR offering_id IS NULL) "
                        + "AND start_date <= ? AND end_date >= ? ORDER BY start_date",
                statement -> {
                    statement.setInt(1, studentId);
                    if (offeringId == null) {
                        statement.setNull(2, java.sql.Types.INTEGER);
                        statement.setNull(3, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(2, offeringId);
                        statement.setInt(3, offeringId);
                    }
                    statement.setDate(4, Date.valueOf(endDate));
                    statement.setDate(5, Date.valueOf(startDate));
                }, this::map);
    }

    @Override
    public List<MedicalRow> findApprovedForRange(
            int studentId, int offeringId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return jdbc.query(SELECT_COLUMNS
                        + "WHERE student_id = ? AND (offering_id = ? OR offering_id IS NULL) "
                        + "AND approval_status = 'APPROVED' AND start_date <= ? AND end_date >= ? "
                        + "ORDER BY start_date",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, offeringId);
                    statement.setDate(3, Date.valueOf(endDate));
                    statement.setDate(4, Date.valueOf(startDate));
                }, this::map);
    }

    private MedicalRow map(ResultSet result) throws SQLException {
        int offering = result.getInt("offering_id");
        Integer offeringId = result.wasNull() ? null : offering;
        Date medDate = result.getDate("medical_date");
        Date start = result.getDate("start_date");
        Date end = result.getDate("end_date");
        return new MedicalRow(result.getInt("medical_id"), result.getInt("student_id"), offeringId,
                medDate.toLocalDate(), start.toLocalDate(), end.toLocalDate(),
                result.getString("reason_details"), result.getString("evidence_reference"),
                ApprovalStatus.valueOf(result.getString("approval_status")),
                result.getBoolean("affects_eligibility"));
    }
}
