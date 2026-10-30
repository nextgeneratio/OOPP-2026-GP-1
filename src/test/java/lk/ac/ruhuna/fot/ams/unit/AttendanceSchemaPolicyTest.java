package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AttendanceSchemaPolicyTest {
    @Test
    void canonicalSchemaRequiresTechnicalOfficerRecorder() throws IOException {
        String schema = readResource("db/schema.sql");

        assertThat(schema).contains("recorded_by INT NOT NULL",
                "chk_attendance_sessions_technical_officer_only",
                "CHECK (recorded_by IS NOT NULL AND recorded_by_lecturer IS NULL)");
        assertThat(schema).doesNotContain("chk_attendance_sessions_single_recorder");
    }

    @Test
    void compatibilityMigrationKeepsLecturerAttendanceReadOnly() throws IOException {
        String migration = readResource("db/academic-workflow-compatibility.sql");

        assertThat(migration).contains("MODIFY COLUMN `recorded_by` INT NOT NULL",
                "chk_attendance_sessions_technical_officer_only",
                "CHECK (`recorded_by` IS NOT NULL AND `recorded_by_lecturer` IS NULL)");
        assertThat(migration).doesNotContain("chk_attendance_sessions_single_recorder");
    }

    private String readResource(String name) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IOException("Missing test resource: " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
