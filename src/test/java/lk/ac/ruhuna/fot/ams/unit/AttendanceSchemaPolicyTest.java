package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AttendanceSchemaPolicyTest {
    @Test
    void canonicalSchemaRequiresTechnicalOfficerRecorder() throws IOException {
        String schema = readInstaller();

        assertThat(schema).contains("recorded_by INT NOT NULL",
                "chk_attendance_sessions_technical_officer_only",
                "CHECK (recorded_by IS NOT NULL AND recorded_by_lecturer IS NULL)");
        assertThat(schema).doesNotContain("chk_attendance_sessions_single_recorder");
    }

    @Test
    void releaseInstallerKeepsLecturerAttendanceReadOnly() throws IOException {
        String migration = readInstaller();

        assertThat(migration).contains("recorded_by INT NOT NULL",
                "chk_attendance_sessions_technical_officer_only",
                "CHECK (recorded_by IS NOT NULL AND recorded_by_lecturer IS NULL)");
        assertThat(migration).doesNotContain("chk_attendance_sessions_single_recorder");
    }

    private String readInstaller() throws IOException {
        return Files.readString(Path.of("database", "install.sql"), StandardCharsets.UTF_8);
    }
}
