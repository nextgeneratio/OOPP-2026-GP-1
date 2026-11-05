package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.UserLookup;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.Lecturer;
import lk.ac.ruhuna.fot.ams.domain.model.TechnicalOfficer;
import lk.ac.ruhuna.fot.ams.domain.model.Undergraduate;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class DemoInstallerTest {
    private static final Pattern USER_ROW = Pattern.compile(
            "\\('([^']+)'\\s*,\\s*'([^']+)'\\s*,\\s*'(ADMIN|LECTURER|TECHNICAL_OFFICER|UNDERGRADUATE)'"
                    + "\\s*,\\s*'([^']+)'\\s*,\\s*TRUE\\s*,\\s*NOW\\(\\)\\)");

    @Test
    void everySeededAccountUsesVerifiedHashAndAuthenticatesWithItsExpectedRole() throws IOException {
        String installer = Files.readString(Path.of("database", "install.sql"), StandardCharsets.UTF_8);
        Matcher matcher = USER_ROW.matcher(installer);
        Map<String, SeededUser> seeded = new HashMap<>();
        Set<String> salts = new HashSet<>();
        PasswordHasher hasher = new PasswordHasher();
        while (matcher.find()) {
            String username = matcher.group(1);
            String hash = matcher.group(2);
            Role role = Role.valueOf(matcher.group(3));
            String email = matcher.group(4);
            assertThat(hasher.verify("1234".toCharArray(), hash)).as(username).isTrue();
            assertThat(salts.add(hash.split("\\$", -1)[2])).as("unique salt for " + username).isTrue();
            assertThat(seeded.put(username, new SeededUser(role, hash, email))).isNull();
        }

        Map<String, Role> expected = expectedAccounts();
        assertThat(seeded).containsOnlyKeys(expected.keySet());
        assertThat(seeded).allSatisfy((username, account) ->
                assertThat(account.role()).isEqualTo(expected.get(username)));
        assertThat(salts).hasSize(34);

        Map<String, User> users = new HashMap<>();
        long id = 1;
        for (Map.Entry<String, SeededUser> entry : seeded.entrySet()) {
            String username = entry.getKey();
            SeededUser account = entry.getValue();
            User user = switch (account.role()) {
                case ADMIN -> new Admin(id, username, account.hash(), account.email(), true);
                case LECTURER -> new Lecturer(id, username, account.hash(), account.email(), true, 1);
                case TECHNICAL_OFFICER ->
                        new TechnicalOfficer(id, username, account.hash(), account.email(), true, 1);
                case UNDERGRADUATE ->
                        new Undergraduate(id, username, account.hash(), account.email(), true, (int) id, 1,
                                username);
            };
            users.put(username, user);
            id++;
        }
        UserLookup lookup = lookup(users);
        AuthService authentication = new AuthService(lookup, hasher, (userId, successful) -> { });
        for (Map.Entry<String, Role> entry : expected.entrySet()) {
            char[] password = "1234".toCharArray();
            AuthenticatedSession session = authentication.authenticate(entry.getKey(), password);
            assertThat(session.username()).isEqualTo(entry.getKey());
            assertThat(session.role()).isEqualTo(entry.getValue());
            assertThat(password).containsOnly('\0');
        }

        assertThatThrownBy(() -> hasher.hash("1234".toCharArray()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void installerUsesRoleProfilesAndSafeAllTableReplacement() throws IOException {
        String installer = Files.readString(Path.of("database", "install.sql"), StandardCharsets.UTF_8);
        assertThat(count(installer, "CREATE TABLE IF NOT EXISTS")).isEqualTo(30);
        assertThat(count(installer, "DELETE FROM `")).isEqualTo(30);
        assertThat(installer.toUpperCase()).doesNotContain("DROP TABLE", "DROP DATABASE", "TRUNCATE TABLE");
        assertThat(installer).contains("INSERT INTO `admins`", "WHERE username='admin'",
                "INSERT INTO `lecturers`", "INSERT INTO `technical_officers`",
                "WHERE u.role='TECHNICAL_OFFICER'", "INSERT INTO `undergraduates`",
                "WHERE u.role='UNDERGRADUATE'", "student_no,full_name,profile_picture",
                "SELECT u.user_id,b.batch_id,u.username");
        String lecturers = between(installer, "INSERT INTO `lecturers`", "INSERT INTO `technical_officers`");
        for (int i = 1; i <= 9; i++) assertThat(lecturers).contains("'LE%03d'".formatted(i));
        assertThat(installer).doesNotContain("DEMO_ADMIN", "DEMO_LEC", "DEMO_TO", "DEMO_STU",
                "ADM001", "LEC001", "TO001", "STU001");
        assertThat(installer).contains("'ST001'", "'TE001'", "'LE001'");
        assertThat(installer).contains("CREATE DATABASE IF NOT EXISTS `university_management`",
                "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci",
                "application column types or nullability do not match",
                "required application index is missing", "application constraints do not match",
                "RESIGNAL", "START TRANSACTION", "COMMIT", "ROLLBACK");
    }

    @Test
    void allManifestPdfsExistAndAreRegisteredWithTheirFinalUploader() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("docs", "generated-course-materials-manifest.csv"),
                StandardCharsets.UTF_8);
        String installer = Files.readString(Path.of("database", "install.sql"), StandardCharsets.UTF_8);
        String materials = between(installer, "INSERT INTO `course_materials`", "INSERT INTO `grades`");
        assertThat(lines).hasSize(46);
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split(",", -1);
            assertThat(fields).hasSize(9);
            String path = fields[3];
            String uploader = fields[7];
            Path pdf = Path.of("src", "main", "resources").resolve(path);
            assertThat(Files.isRegularFile(pdf)).as(path).isTrue();
            assertThat(Files.readString(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF-")).as(path).isTrue();
            assertThat(materials).contains("'" + path + "'", "'" + uploader + "'");
            assertThat(fields[8]).isEqualTo("TRUE");
        }
    }

    private Map<String, Role> expectedAccounts() {
        Map<String, Role> expected = new HashMap<>();
        expected.put("admin", Role.ADMIN);
        for (int i = 1; i <= 9; i++) expected.put("LE%03d".formatted(i), Role.LECTURER);
        for (int i = 1; i <= 4; i++) expected.put("TE%03d".formatted(i), Role.TECHNICAL_OFFICER);
        for (int i = 1; i <= 20; i++) expected.put("ST%03d".formatted(i), Role.UNDERGRADUATE);
        return expected;
    }

    private UserLookup lookup(Map<String, User> users) {
        return new UserLookup() {
            @Override public Optional<User> findActiveByUsername(String username) {
                return Optional.ofNullable(users.get(username)).filter(User::isActive);
            }
            @Override public Optional<User> findByUsername(String username) {
                return Optional.ofNullable(users.get(username));
            }
        };
    }

    private int count(String value, String needle) {
        return value.split(Pattern.quote(needle), -1).length - 1;
    }

    private String between(String value, String start, String end) {
        int from = value.indexOf(start);
        int to = value.indexOf(end, from + start.length());
        if (from < 0 || to < 0) throw new AssertionError("Missing installer section: " + start);
        return value.substring(from, to);
    }

    private record SeededUser(Role role, String hash, String email) { }
}
