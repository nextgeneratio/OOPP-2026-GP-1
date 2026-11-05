package lk.ac.ruhuna.fot.ams.business.service;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

/** Stores validated profile pictures below the application resources upload directory. */
public final class ProfilePictureStore {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path root;

    public ProfilePictureStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public String save(long userId, String fileName, byte[] contents) {
        if (contents == null || contents.length == 0 || contents.length > MAX_BYTES) {
            throw new ValidationException("Choose a profile picture smaller than 5 MB.");
        }
        String extension = extension(fileName);
        if (!extension.equals("png") && !extension.equals("jpg") && !extension.equals("jpeg")) {
            throw new ValidationException("Profile pictures must be PNG or JPEG files.");
        }
        try {
            if (ImageIO.read(new ByteArrayInputStream(contents)) == null) {
                throw new ValidationException("The selected file is not a valid image.");
            }
            Path directory = root.resolve("profile-pictures");
            Files.createDirectories(directory);
            String storedName = "user-" + userId + "-" + UUID.randomUUID() + "." + extension;
            Files.write(directory.resolve(storedName), contents);
            return "uploads/profile-pictures/" + storedName;
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to save the profile picture.", failure);
        }
    }

    public ImageIcon icon(String storedReference, int width, int height) {
        if (storedReference == null || storedReference.isBlank()) {
            return null;
        }
        try {
            Path relative = Path.of(storedReference);
            if (relative.isAbsolute() || relative.normalize().startsWith("..")
                    || relative.getNameCount() != 3
                    || !relative.getName(0).toString().equals("uploads")
                    || !relative.getName(1).toString().equals("profile-pictures")) {
                return null;
            }
            Path file = root.resolve(relative.subpath(1, relative.getNameCount())).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                return null;
            }
            Image image = ImageIO.read(file.toFile());
            return image == null ? null : new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH));
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }

    private String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot < 0 ? "" : lower.substring(dot + 1);
    }
}
