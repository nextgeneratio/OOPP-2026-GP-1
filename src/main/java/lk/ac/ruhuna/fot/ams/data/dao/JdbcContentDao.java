package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public final class JdbcContentDao implements ContentDao {
    private final JdbcExecutor jdbc;

    public JdbcContentDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertMaterial(Connection connection, MaterialRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_materials (offering_id, lecturer_id, title, file_path) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.offeringId());
                    statement.setInt(2, row.lecturerId());
                    statement.setString(3, row.title());
                    statement.setString(4, row.filePath());
                });
    }

    @Override
    public int updateMaterial(Connection connection, MaterialRow row) {
        return jdbc.update(connection,
                "UPDATE course_materials SET title = ?, file_path = ? "
                        + "WHERE material_id = ? AND lecturer_id = ? AND offering_id = ?",
                statement -> {
                    statement.setString(1, row.title());
                    statement.setString(2, row.filePath());
                    statement.setInt(3, row.id());
                    statement.setInt(4, row.lecturerId());
                    statement.setInt(5, row.offeringId());
                });
    }

    @Override
    public List<MaterialRow> findMaterialsForOffering(int offeringId) {
        return jdbc.query("SELECT material_id, offering_id, lecturer_id, title, file_path "
                        + "FROM course_materials WHERE offering_id = ? ORDER BY material_id DESC",
                statement -> statement.setInt(1, offeringId), this::mapMaterial);
    }

    @Override
    public List<MaterialRow> findMaterialsForStudent(int studentId, int offeringId) {
        return jdbc.query("SELECT m.material_id, m.offering_id, m.lecturer_id, m.title, m.file_path "
                        + "FROM course_materials m JOIN course_enrollments e ON e.offering_id = m.offering_id "
                        + "WHERE e.student_id = ? AND e.offering_id = ? AND e.enrollment_status = 'ACTIVE' "
                        + "ORDER BY m.material_id DESC",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, offeringId);
                }, this::mapMaterial);
    }

    @Override
    public long insertNotice(Connection connection, NoticeRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO notices (title, body, audience, target_role, target_batch_id, publish_date, expiry_date) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, row.title());
                    statement.setString(2, row.body());
                    statement.setString(3, row.audience());
                    if (row.targetRole() == null) {
                        statement.setNull(4, java.sql.Types.VARCHAR);
                    } else {
                        statement.setString(4, row.targetRole().name());
                    }
                    if (row.targetBatchId() == null) {
                        statement.setNull(5, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(5, row.targetBatchId());
                    }
                    statement.setTimestamp(6, Timestamp.valueOf(row.publishDate()));
                    if (row.expiryDate() == null) {
                        statement.setNull(7, java.sql.Types.TIMESTAMP);
                    } else {
                        statement.setTimestamp(7, Timestamp.valueOf(row.expiryDate()));
                    }
                });
    }

    @Override
    public List<NoticeRow> findVisibleNotices(Role role, Integer batchId, java.time.LocalDateTime asOf) {
        String sql = "SELECT notice_id, title, body, audience, target_role, target_batch_id, publish_date, expiry_date "
                + "FROM notices WHERE publish_date <= ? AND (expiry_date IS NULL OR expiry_date > ?) "
                + "AND (audience = 'ALL' OR (audience = 'ROLE' AND target_role = ?) "
                + "OR (audience = 'BATCH' AND target_batch_id = ?)) ORDER BY publish_date DESC";
        return jdbc.query(sql, statement -> {
            statement.setTimestamp(1, Timestamp.valueOf(asOf));
            statement.setTimestamp(2, Timestamp.valueOf(asOf));
            statement.setString(3, role.name());
            if (batchId == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, batchId);
            }
        }, this::mapNotice);
    }

    private MaterialRow mapMaterial(ResultSet result) throws SQLException {
        return new MaterialRow(result.getInt("material_id"), result.getInt("offering_id"),
                result.getInt("lecturer_id"), result.getString("title"), result.getString("file_path"));
    }

    private NoticeRow mapNotice(ResultSet result) throws SQLException {
        String target = result.getString("target_role");
        int batch = result.getInt("target_batch_id");
        Integer batchId = result.wasNull() ? null : batch;
        Timestamp expiry = result.getTimestamp("expiry_date");
        return new NoticeRow(result.getInt("notice_id"), result.getString("title"), result.getString("body"),
                result.getString("audience"), target == null ? null : Role.valueOf(target), batchId,
                result.getTimestamp("publish_date").toLocalDateTime(),
                expiry == null ? null : expiry.toLocalDateTime());
    }
}
