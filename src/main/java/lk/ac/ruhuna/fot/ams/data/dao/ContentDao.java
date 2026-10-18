package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public interface ContentDao {
    record MaterialRow(int id, int offeringId, int lecturerId, String title, String filePath) {
    }

    record NoticeRow(
            int id,
            String title,
            String body,
            String audience,
            Role targetRole,
            Integer targetBatchId,
            LocalDateTime publishDate,
            LocalDateTime expiryDate) {
    }

    long insertMaterial(Connection connection, MaterialRow row);

    int updateMaterial(Connection connection, MaterialRow row);

    List<MaterialRow> findMaterialsForOffering(int offeringId);

    List<MaterialRow> findMaterialsForStudent(int studentId, int offeringId);

    long insertNotice(Connection connection, NoticeRow row);

    List<NoticeRow> findVisibleNotices(Role role, Integer batchId, LocalDateTime asOf);
}
