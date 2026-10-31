package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import lk.ac.ruhuna.fot.ams.business.service.ContentService;
import lk.ac.ruhuna.fot.ams.business.service.MaterialFileStore;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

class ContentMaterialVisibilityTest {
    @TempDir Path temp;
    private ContentDao dao;
    private AuthorizationScopeLookup scopes;
    private ContentService service;
    private Path storageRoot;
    private final AuthenticatedSession student =
            new AuthenticatedSession(70, "demo-student", Role.UNDERGRADUATE, "Synthetic Student");
    private final AuthenticatedSession lecturer =
            new AuthenticatedSession(71, "demo-lecturer", Role.LECTURER, "Synthetic Lecturer");

    @BeforeEach void setUp() throws IOException {
        dao = mock(ContentDao.class);
        scopes = mock(AuthorizationScopeLookup.class);
        when(scopes.undergraduateEnrolledInOffering(70, 12)).thenReturn(true);
        when(scopes.studentIdForUser(70)).thenReturn(OptionalInt.of(5));
        when(scopes.lecturerAssignedToOffering(71, 12)).thenReturn(true);
        when(scopes.lecturerIdForUser(71)).thenReturn(OptionalInt.of(3));
        Path resources = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resources.resolve("uploads"));
        storageRoot = root;
        service = new ContentService(dao, mock(CourseDao.class), mock(AuditLogDao.class),
                new TransactionManager(null), new AuthorizationService(scopes), Clock.systemUTC(),
                new MaterialFileStore(root));
    }

    @Test void onlyVisibleEnrolledMaterialsWithExistingReadableFilesAreReturned() throws IOException {
        Path valid = Files.writeString(temp.resolve("resources/uploads/valid.pdf"), "synthetic PDF bytes");
        when(dao.findMaterialsForStudent(5, 12)).thenReturn(List.of(
                new ContentDao.MaterialRow(1, 12, 3, "Valid", "uploads/valid.pdf", true),
                new ContentDao.MaterialRow(2, 12, 3, "Missing", "uploads/missing.pdf", true),
                new ContentDao.MaterialRow(3, 12, 3, "Unpublished", "uploads/valid.pdf", false)));
        assertThat(service.materialsForStudent(student, 12)).extracting(ContentDao.MaterialRow::id)
                .containsExactly(1);
        assertThat(Files.isReadable(valid)).isTrue();
    }

    @Test void rejectsStudentOutsideOfferingScope() {
        assertThatThrownBy(() -> service.materialsForStudent(student, 13))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test void openRechecksFileAfterTheListWasLoaded() throws IOException {
        Path file = Files.writeString(temp.resolve("resources/uploads/gone.pdf"), "synthetic");
        when(dao.findMaterialsForStudent(5, 12)).thenReturn(List.of(
                new ContentDao.MaterialRow(9, 12, 3, "Soon gone", "uploads/gone.pdf", true)));
        assertThat(service.materialsForStudent(student, 12)).hasSize(1);
        Files.delete(file);
        assertThatThrownBy(() -> service.studentMaterialFile(student, 12, 9))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no longer available");
    }

    @Test void doesNotOpenMaterialsThatWereNotPublishedOrAuthorizedByTheDao() {
        when(dao.findMaterialsForStudent(5, 12)).thenReturn(List.of());
        assertThatThrownBy(() -> service.studentMaterialFile(student, 12, 10))
                .isInstanceOf(ValidationException.class);
    }

    @Test void assignedLecturerCanIdentifyAnIncompleteResourceWithoutPublishingIt() {
        when(dao.findMaterialsForOffering(12)).thenReturn(List.of(
                new ContentDao.MaterialRow(22, 12, 3, "Missing file", "uploads/no-file.pdf", true)));
        assertThat(service.lecturerMaterialFileAvailable(lecturer, 12, 22)).isFalse();
    }

    @Test void publishedMaterialRemainsVisibleAfterServiceReconstruction() throws IOException {
        Files.writeString(storageRoot.resolve("notes.pdf"), "synthetic PDF");
        when(dao.findMaterialsForStudent(5, 12)).thenReturn(List.of(
                new ContentDao.MaterialRow(31, 12, 3, "Persistent", "uploads/notes.pdf", true)));
        ContentService restarted = new ContentService(dao, mock(CourseDao.class), mock(AuditLogDao.class),
                new TransactionManager(null), new AuthorizationService(scopes), Clock.systemUTC(),
                new MaterialFileStore(storageRoot));
        assertThat(restarted.materialsForStudent(student, 12))
                .extracting(ContentDao.MaterialRow::id).containsExactly(31);
    }
}
