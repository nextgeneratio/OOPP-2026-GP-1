package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.MaterialRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class CourseMaterialsScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final ContentController content;

    public CourseMaterialsScreen(AuthenticatedSession actor, ContentController content) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.content = content;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Course materials</h2>Lecturers upload materials hidden by default and "
                + "publish them when ready. Students only see published items.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        if (actor.role() == Role.LECTURER || actor.role() == Role.TECHNICAL_OFFICER) {
            button(actions, "Upload material", this::upload);
            button(actions, "Edit material", this::editMaterial);
            button(actions, "Hide / publish material", this::toggleVisibility);
            button(actions, "List my offering materials", this::listLecturer);
        } else {
            button(actions, "View published materials", this::listStudent);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void upload() {
        JTextField offering = new JTextField();
        JTextField title = new JTextField();
        JFileChooser chooser = new JFileChooser();
        if (JOptionPane.showConfirmDialog(this, fields("Offering ID", offering, "Title", title),
                "Upload material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path copied = copyUpload(chooser.getSelectedFile().toPath());
        String path = Path.of("uploads", "materials", copied.getFileName().toString())
                .toString().replace('\\', '/');
        ApiResponse<?> response = content.addMaterial(actor, new ContentController.MaterialRequest(
                integer(offering), title.getText(), path));
        if (!response.successful()) {
            try {
                Files.deleteIfExists(copied);
            } catch (IOException cleanupFailure) {
                error("Upload failed and the temporary file could not be removed: " + cleanupFailure.getMessage());
            }
        }
        show(response);
    }

    private void editMaterial() {
        JTextField offering = new JTextField();
        JTextField material = new JTextField();
        JTextField title = new JTextField();
        JFileChooser chooser = new JFileChooser();
        if (JOptionPane.showConfirmDialog(this, fields("Offering ID", offering,
                "Material ID", material, "New title", title), "Edit material",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path copied = copyUpload(chooser.getSelectedFile().toPath());
        String path = Path.of("uploads", "materials", copied.getFileName().toString())
                .toString().replace('\\', '/');
        ApiResponse<?> response = content.updateMaterial(actor, new ContentController.UpdateMaterialRequest(
                integer(offering), integer(material), title.getText(), path));
        if (!response.successful()) {
            try {
                Files.deleteIfExists(copied);
            } catch (IOException cleanupFailure) {
                error("Update failed and the temporary file could not be removed: " + cleanupFailure.getMessage());
            }
        }
        show(response);
    }

    private void toggleVisibility() {
        JTextField offering = new JTextField();
        JTextField material = new JTextField();
        JTextField visible = new JTextField("true");
        if (JOptionPane.showConfirmDialog(this, fields("Offering ID", offering, "Material ID", material,
                "Visible true/false", visible), "Change material visibility",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String value = visible.getText().trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Visible must be true or false.");
        }
        show(content.setMaterialVisibility(actor, new ContentController.MaterialVisibilityRequest(
                integer(offering), integer(material), Boolean.parseBoolean(value))));
    }

    private void listLecturer() {
        JTextField offering = new JTextField();
        if (JOptionPane.showConfirmDialog(this, fields("Offering ID", offering), "List materials",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        ApiResponse<java.util.List<MaterialRow>> result =
                content.materialsForLecturer(actor, integer(offering));
        displayMaterials(result);
    }

    private void listStudent() {
        JTextField offering = new JTextField();
        if (JOptionPane.showConfirmDialog(this, fields("Enrolled offering ID", offering), "View materials",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        ApiResponse<java.util.List<MaterialRow>> result =
                content.materialsForStudent(actor, integer(offering));
        displayMaterials(result);
    }

    private void displayMaterials(ApiResponse<java.util.List<MaterialRow>> result) {
        if (!result.successful()) {
            error(result.error().message());
            return;
        }
        JTextArea output = new JTextArea(14, 60);
        output.setEditable(false);
        result.data().forEach(item -> output.append(String.format(
                "%d | %s | %s | %s%n", item.id(), item.title(), item.visible() ? "published" : "hidden",
                item.filePath())));
        if (JOptionPane.showConfirmDialog(this, new javax.swing.JScrollPane(output),
                "Materials",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
                && actor.role() == Role.UNDERGRADUATE) {
            JTextField selectedId = new JTextField();
            if (JOptionPane.showConfirmDialog(this, fields("Material ID to open", selectedId),
                    "Open material", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                int id = integer(selectedId);
                result.data().stream().filter(item -> item.id() == id).findFirst()
                        .ifPresentOrElse(this::openMaterial, () -> error("Choose a material ID from the list."));
            }
        }
    }

    private void openMaterial(MaterialRow material) {
        try {
            Path path = Path.of(material.filePath()).normalize();
            if (path.isAbsolute() || path.startsWith("..") || !path.startsWith("uploads")) {
                error("Stored material path is invalid.");
                return;
            }
            Path file = path.toAbsolutePath().normalize();
            if (!Files.isRegularFile(file)) {
                error("The material file is missing from this installation.");
                return;
            }
            Desktop.getDesktop().open(file.toFile());
        } catch (IOException | UnsupportedOperationException failure) {
            error("Unable to open the selected material: " + failure.getMessage());
        }
    }

    private Path copyUpload(Path source) {
        try {
            if (!Files.isRegularFile(source) || Files.size(source) > 25L * 1024 * 1024) {
                throw new IllegalArgumentException("Choose a readable material file smaller than 25 MB.");
            }
            String name = source.getFileName().toString();
            String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            if (!extension.matches("(?i)\\.(pdf|doc|docx|ppt|pptx|xls|xlsx|txt|png|jpe?g|mp4)$")) {
                throw new IllegalArgumentException("Choose a supported PDF, Office, text, image, or video file.");
            }
            Path directory = Path.of("uploads", "materials").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            return Files.copy(source, directory.resolve(UUID.randomUUID() + extension));
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to store this material file.", failure);
        }
    }

    private JPanel fields(Object... pairs) {
        JPanel panel = new JPanel(new GridLayout(pairs.length / 2, 2, 8, 8));
        for (int index = 0; index < pairs.length; index += 2) {
            panel.add(new JLabel((String) pairs[index]));
            panel.add((JTextField) pairs[index + 1]);
        }
        return panel;
    }

    private void button(JPanel panel, String text, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(text);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (IllegalArgumentException | IllegalStateException invalid) {
                error("Check the entered IDs and selected file: " + invalid.getMessage());
            }
        });
        panel.add(button);
    }

    private int integer(JTextField field) {
        return Integer.parseInt(field.getText().trim());
    }

    private void show(ApiResponse<?> response) {
        if (response.successful()) {
            JOptionPane.showMessageDialog(this, "Action completed successfully.");
        } else {
            error(response.error().message());
        }
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Course materials", JOptionPane.ERROR_MESSAGE);
    }
}
