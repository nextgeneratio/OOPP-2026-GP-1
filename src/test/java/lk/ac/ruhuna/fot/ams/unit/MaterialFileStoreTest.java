package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import lk.ac.ruhuna.fot.ams.business.service.MaterialFileStore;

class MaterialFileStoreTest {
    @TempDir Path temp;

    @Test void acceptsReadableRegularFileUnderRoot() throws IOException {
        Path resourceRoot = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resourceRoot.resolve("uploads"));
        Path file = Files.createDirectories(root.resolve("materials")).resolve("notes.pdf");
        Files.writeString(file, "%PDF-1.4 synthetic test content");
        assertThat(new MaterialFileStore(root).resolveReadable("uploads/materials/notes.pdf"))
                .contains(file.toRealPath());
    }

    @Test void rejectsMissingFilesDirectoriesAndBadReferences() throws IOException {
        Path resources = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resources.resolve("uploads"));
        Files.createDirectories(root.resolve("materials/folder"));
        MaterialFileStore store = new MaterialFileStore(root);
        assertThat(store.resolveReadable("uploads/materials/missing.pdf")).isEmpty();
        assertThat(store.resolveReadable("uploads/materials/folder")).isEmpty();
        assertThat(store.resolveReadable("../outside.pdf")).isEmpty();
        assertThat(store.resolveReadable("elsewhere/secret.pdf")).isEmpty();
        assertThat(store.resolveReadable("")).isEmpty();
    }

    @Test void rejectsSymlinkEscapingStorageRoot() throws IOException {
        Path resources = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resources.resolve("uploads"));
        Path outside = Files.writeString(temp.resolve("outside.pdf"), "outside");
        Path directory = Files.createDirectories(root.resolve("materials"));
        try {
            Files.createSymbolicLink(directory.resolve("escape.pdf"), outside);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            return; // Filesystems without symlink support cannot exercise this case.
        }
        assertThat(new MaterialFileStore(root).resolveReadable("uploads/materials/escape.pdf")).isEmpty();
    }

    @Test void rechecksExistenceAfterListWasLoaded() throws IOException {
        Path resources = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resources.resolve("uploads"));
        Path file = Files.createDirectories(root.resolve("materials")).resolve("removed.pdf");
        Files.writeString(file, "synthetic");
        MaterialFileStore store = new MaterialFileStore(root);
        assertThat(store.isReadable("uploads/materials/removed.pdf")).isTrue();
        Files.delete(file);
        assertThat(store.resolveReadable("uploads/materials/removed.pdf")).isEmpty();
    }

    @Test void rejectsUnreadableRegularFiles() throws IOException {
        Path resources = Files.createDirectory(temp.resolve("resources"));
        Path root = Files.createDirectories(resources.resolve("uploads/materials"));
        Path file = Files.writeString(root.resolve("private.pdf"), "synthetic");
        Assumptions.assumeTrue(Files.getFileStore(file).supportsFileAttributeView("posix"));
        Set<PosixFilePermission> original = Files.getPosixFilePermissions(file);
        try {
            Files.setPosixFilePermissions(file, Set.of());
            Assumptions.assumeFalse(Files.isReadable(file), "the current process can still read mode-000 files");
            assertThat(new MaterialFileStore(resources.resolve("uploads"))
                    .resolveReadable("uploads/materials/private.pdf")).isEmpty();
        } finally {
            Files.setPosixFilePermissions(file, original);
        }
    }
}
