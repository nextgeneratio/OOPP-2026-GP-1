package lk.ac.ruhuna.fot.ams.unit;

import lk.ac.ruhuna.fot.ams.audit.AuditLog;
import lk.ac.ruhuna.fot.ams.audit.AuditService;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.UserLookup;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.Lecturer;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAndAuditVerificationTest {

    private PasswordHasher passwordHasher;
    private List<AuditLog> capturedLogs;
    private AuditService auditService;
    private UserLookup mockUserLookup;

    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasher();
        capturedLogs = new ArrayList<>();
        auditService = capturedLogs::add;
    }

    @Test
    @DisplayName("Password storage must use PBKDF2 with salt and must not equal plaintext")
    void passwordSecurity_shouldUseSaltedHash() {
        char[] rawPassword = "Admin@Secure2026".toCharArray();
        String hash1 = passwordHasher.hash(rawPassword);
        String hash2 = passwordHasher.hash(rawPassword);

        // Plaintext is never stored
        assertThat(hash1).doesNotContain("Admin@Secure2026");
        assertThat(hash1).startsWith("PBKDF2$210000$");

        // Salt creates different hashes for identical passwords
        assertThat(hash1).isNotEqualTo(hash2);

        // Verification succeeds with correct password
        assertThat(passwordHasher.verify("Admin@Secure2026".toCharArray(), hash1)).isTrue();
        // Verification fails with wrong password
        assertThat(passwordHasher.verify("WrongPassword!".toCharArray(), hash1)).isFalse();
    }

    @Test
    @DisplayName("AuthService must emit LOGIN_SUCCESS audit log upon successful login")
    void authService_auditSuccessfulLogin() {
        String hash = passwordHasher.hash("ValidPass123!".toCharArray());
        User user = new Lecturer(101L, "TG001", hash, "tg001@fot.ruh.ac.lk", true, 1);

        mockUserLookup = username -> "TG001".equals(username) ? Optional.of(user) : Optional.empty();
        AuthService authService = new AuthService(mockUserLookup, passwordHasher, auditService);

        authService.authenticate("TG001", "ValidPass123!".toCharArray());

        assertThat(capturedLogs).hasSize(1);
        AuditLog log = capturedLogs.get(0);
        assertThat(log.userId()).isEqualTo(101L);
        assertThat(log.action()).isEqualTo("LOGIN_SUCCESS");
        assertThat(log.entityName()).isEqualTo("users");
        assertThat(log.entityId()).isEqualTo(101L);
        assertThat(log.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("AuthService must emit LOGIN_FAILED audit log upon invalid credentials")
    void authService_auditFailedLogin_wrongPassword() {
        String hash = passwordHasher.hash("ValidPass123!".toCharArray());
        User user = new Admin(102L, "admin_user", hash, "admin@fot.ruh.ac.lk", true);

        mockUserLookup = username -> "admin_user".equals(username) ? Optional.of(user) : Optional.empty();
        AuthService authService = new AuthService(mockUserLookup, passwordHasher, auditService);

        assertThatThrownBy(() -> authService.authenticate("admin_user", "WrongPassword999!".toCharArray()))
                .isInstanceOf(AuthenticationException.class);

        assertThat(capturedLogs).hasSize(1);
        AuditLog log = capturedLogs.get(0);
        assertThat(log.userId()).isEqualTo(102L);
        assertThat(log.action()).isEqualTo("LOGIN_FAILED");
        assertThat(log.entityName()).isEqualTo("users");
    }

    @Test
    @DisplayName("AuthService must emit LOGIN_FAILED audit log when username does not exist")
    void authService_auditFailedLogin_unknownUser() {
        mockUserLookup = username -> Optional.empty();
        AuthService authService = new AuthService(mockUserLookup, passwordHasher, auditService);

        assertThatThrownBy(() -> authService.authenticate("UNKNOWN_USER", "SomePassword123!".toCharArray()))
                .isInstanceOf(AuthenticationException.class);

        assertThat(capturedLogs).hasSize(1);
        AuditLog log = capturedLogs.get(0);
        assertThat(log.userId()).isNull();
        assertThat(log.action()).isEqualTo("LOGIN_FAILED");
    }

    @Test
    @DisplayName("AuditLog record supports creation, modification, attendance, and medical actions")
    void auditLog_structureCoverage() {
        AuditLog marksLog = new AuditLog(201L, "MARKS_UPDATE", "marks", 501L);
        assertThat(marksLog.userId()).isEqualTo(201L);
        assertThat(marksLog.action()).isEqualTo("MARKS_UPDATE");
        assertThat(marksLog.entityName()).isEqualTo("marks");
        assertThat(marksLog.entityId()).isEqualTo(501L);
        assertThat(marksLog.occurredAt()).isNotNull();

        AuditLog attendanceLog = new AuditLog(202L, "ATTENDANCE_RECORD", "attendance_records", 601L);
        assertThat(attendanceLog.action()).isEqualTo("ATTENDANCE_RECORD");

        AuditLog medicalLog = new AuditLog(203L, "MEDICAL_APPROVE", "medical_records", 701L);
        assertThat(medicalLog.action()).isEqualTo("MEDICAL_APPROVE");
    }
}
