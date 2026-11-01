package lk.ac.ruhuna.fot.ams.business.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/** Resolves opaque material references only to readable regular files under the configured root. */
public final class MaterialFileStore {
    private final Path root;

    public MaterialFileStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Optional<Path> resolveReadable(String storedReference) {
        if (storedReference == null || storedReference.isBlank()) return Optional.empty();
        try {
            Path relative = Path.of(storedReference);
            if (relative.isAbsolute() || relative.normalize().startsWith("..")
                    || relative.getNameCount() < 2 || !relative.getName(0).toString().equals("uploads")) {
                return Optional.empty();
            }
            Path configuredRoot = root.toRealPath();
            Path candidate = root.resolve(relative.subpath(1, relative.getNameCount())).normalize();
            if (!candidate.startsWith(root)) return Optional.empty();
            Path actual = candidate.toRealPath();
            if (!actual.startsWith(configuredRoot)
                    || !Files.isRegularFile(actual) || !Files.isReadable(actual)) return Optional.empty();
            return Optional.of(actual);
        } catch (IOException | InvalidPathException | SecurityException failure) {
            return Optional.empty();
        }
    }

    public boolean isReadable(String storedReference) {
        return resolveReadable(storedReference).isPresent();
    }
}
