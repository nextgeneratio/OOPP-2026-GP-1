package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcGradeDao implements GradeDao {
    private final JdbcExecutor jdbc;

    public JdbcGradeDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int insert(Connection connection, GradeRow row) {
        return jdbc.update(connection,
                "INSERT INTO grades (scheme_version, grade_letter, min_mark, max_mark, grade_point) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, row.schemeVersion());
                    statement.setString(2, row.letter());
                    statement.setBigDecimal(3, row.minimumMark());
                    statement.setBigDecimal(4, row.maximumMark());
                    statement.setBigDecimal(5, row.gradePoint());
                });
    }

    @Override
    public List<GradeRow> findScheme(String schemeVersion) {
        return jdbc.query("SELECT grade_id, scheme_version, grade_letter, min_mark, max_mark, grade_point "
                        + "FROM grades WHERE scheme_version = ? ORDER BY min_mark DESC",
                statement -> statement.setString(1, schemeVersion), this::map);
    }

    private GradeRow map(ResultSet result) throws SQLException {
        return new GradeRow(result.getInt("grade_id"), result.getString("scheme_version"),
                result.getString("grade_letter"), result.getBigDecimal("min_mark"),
                result.getBigDecimal("max_mark"), result.getBigDecimal("grade_point"));
    }
}
