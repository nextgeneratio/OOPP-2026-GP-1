package lk.ac.ruhuna.fot.ams.data.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public interface GradeDao {
    record GradeRow(
            int id, String schemeVersion, String letter, BigDecimal minimumMark, BigDecimal maximumMark,
            BigDecimal gradePoint) {
    }

    int insert(Connection connection, GradeRow row);

    List<GradeRow> findScheme(String schemeVersion);
}
