package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.MaterialRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Course-first material browser. Students never enter offering or material IDs. */
public final class CourseMaterialsScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final ContentController content;
    private final CourseController courses;
    private final JPanel courseCards = new JPanel();
    private final javax.swing.DefaultListModel<MaterialRow> materialModel = new javax.swing.DefaultListModel<>();
    private final JList<MaterialRow> materialList = new JList<>(materialModel);
    private final JLabel selectedCourse = new JLabel("Choose a course to see its published study material.");
    private final JComboBox<OfferingView> lecturerOffering = new JComboBox<>();
    private final javax.swing.DefaultListModel<String> lecturerMaterialModel = new javax.swing.DefaultListModel<>();

    public CourseMaterialsScreen(AuthenticatedSession actor, ContentController content, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor; this.content = content; this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        if (actor.role() == Role.UNDERGRADUATE) buildStudentBrowser(); else buildLecturerActions();
    }

    private void buildStudentBrowser() {
        add(new JLabel("<html><h2>Course materials</h2>Select one of your courses, then click a material to view it inside the application.</html>"), BorderLayout.NORTH);
        courseCards.setLayout(new javax.swing.BoxLayout(courseCards, javax.swing.BoxLayout.Y_AXIS));
        materialList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        materialList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof MaterialRow material) setText(material.title() + "  -  " + extension(material.filePath()).toUpperCase());
                return this;
            }
        });
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createTitledBorder("Published study material"));
        right.add(selectedCourse, BorderLayout.NORTH); right.add(new JScrollPane(materialList), BorderLayout.CENTER);
        JButton view = new JButton("View selected material"); view.addActionListener(event -> viewSelected());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT)); controls.add(view); right.add(controls, BorderLayout.SOUTH);
        javax.swing.JSplitPane split = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(courseCards), right); split.setResizeWeight(.38); add(split, BorderLayout.CENTER);
        loadCourses();
    }

    private void loadCourses() {
        var response = courses.offeringViewsForActor(actor);
        if (!response.successful()) { error(response.error().message()); return; }
        response.data().forEach(this::addCourseCard);
        if (response.data().isEmpty()) courseCards.add(new JLabel("You are not registered for a course yet."));
    }

    private void addCourseCard(OfferingView offering) {
        JButton card = new JButton("<html><b>" + offering.course().code() + "</b><br/>" + offering.course().title() + "</html>");
        card.setHorizontalAlignment(JButton.LEFT); card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 66));
        card.addActionListener(event -> loadMaterials(offering));
        courseCards.add(card); courseCards.add(javax.swing.Box.createVerticalStrut(8));
    }

    private void loadMaterials(OfferingView offering) {
        selectedCourse.setText(offering.course().code() + " - " + offering.course().title()); materialModel.clear();
        ApiResponse<java.util.List<MaterialRow>> response = content.materialsForStudent(actor, offering.offering().id());
        if (!response.successful()) { error(response.error().message()); return; }
        response.data().forEach(materialModel::addElement);
        if (materialModel.isEmpty()) selectedCourse.setText(selectedCourse.getText() + " (no published materials yet)");
    }

    private void viewSelected() {
        MaterialRow material = materialList.getSelectedValue();
        if (material == null) { error("Select a study material first."); return; }
        try {
            ApiResponse<Path> resolved = content.studentMaterialFile(actor, material.offeringId(), material.id());
            if (!resolved.successful()) { error("This material file is no longer available."); return; }
            Path file = resolved.data();
            if (material.filePath().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                PdfViewerDialog.show(this, material.title(), file);
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.toFile());
            } else error("Only PDF files can be viewed in this application on this computer.");
        } catch (IOException failure) { error("Unable to open the material: " + failure.getMessage()); }
    }

    private void buildLecturerActions() {
        add(new JLabel("<html><h2>Course materials</h2>Manage resources for your assigned offerings. A missing or invalid file is hidden from students.</html>"), BorderLayout.NORTH);
        lecturerOffering.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof OfferingView offering) {
                    setText(offering.course().code() + " — " + offering.course().title() + " — " + offering.semesterLabel());
                }
                return this;
            }
        });
        SelectionOption.enableSearch(lecturerOffering);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("Offering:")); actions.add(lecturerOffering);
        button(actions, "Refresh my resources", this::refreshLecturerMaterials);
        button(actions, "Upload material", this::upload);
        JList<String> lecturerMaterials = new JList<>(lecturerMaterialModel);
        add(actions, BorderLayout.NORTH);
        add(new JScrollPane(lecturerMaterials), BorderLayout.CENTER);
        var response = courses.offeringViewsForActor(actor);
        if (!response.successful()) { error(response.error().message()); return; }
        response.data().forEach(lecturerOffering::addItem);
        if (lecturerOffering.getItemCount() == 0) {
            lecturerMaterialModel.addElement("No assigned course offerings are available.");
        }
    }

    private void refreshLecturerMaterials() {
        OfferingView offering = (OfferingView) lecturerOffering.getSelectedItem();
        lecturerMaterialModel.clear();
        if (offering == null) { lecturerMaterialModel.addElement("No assigned course offering is selected."); return; }
        ApiResponse<java.util.List<MaterialRow>> response = content.materialsForLecturer(actor, offering.offering().id());
        if (!response.successful()) { error(response.error().message()); return; }
        for (MaterialRow material : response.data()) {
            ApiResponse<Boolean> available = content.lecturerMaterialFileAvailable(
                    actor, offering.offering().id(), material.id());
            boolean fileAvailable = available.successful() && Boolean.TRUE.equals(available.data());
            String status = fileAvailable ? (material.visible() ? "Published" : "Hidden")
                    : "Resource file is missing or invalid. Students cannot access this material.";
            lecturerMaterialModel.addElement(material.title() + " — " + status);
        }
        if (lecturerMaterialModel.isEmpty()) lecturerMaterialModel.addElement("No material records for this offering.");
    }

    private void upload() {
        OfferingView offering = (OfferingView) lecturerOffering.getSelectedItem();
        if (offering == null) { error("Select one of your assigned offerings first."); return; }
        JTextField title = new JTextField(); JFileChooser chooser = new JFileChooser();
        if (JOptionPane.showConfirmDialog(this, fields("Title", title), "Upload material", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        int offeringId = offering.offering().id(); Path copied = copyUpload(offeringId, chooser.getSelectedFile().toPath());
        String stored = Path.of("uploads", "materials", "offering-" + offeringId, copied.getFileName().toString()).toString().replace('\\', '/');
        ApiResponse<?> result = content.addMaterial(actor, new ContentController.MaterialRequest(offeringId, title.getText(), stored));
        if (!result.successful()) try { Files.deleteIfExists(copied); } catch (IOException ignored) { }
        show(result);
    }

    private Path copyUpload(int offeringId, Path source) {
        try {
            if (!Files.isRegularFile(source) || Files.size(source) > 25L * 1024 * 1024) throw new IllegalArgumentException("Choose a readable file smaller than 25 MB.");
            String name = source.getFileName().toString(); String extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            if (!extension.matches("(?i)\\.(pdf|doc|docx|ppt|pptx|xls|xlsx|txt|png|jpe?g|mp4)$")) throw new IllegalArgumentException("Unsupported material type.");
            Path directory = Path.of("src", "main", "resources", "uploads", "materials", "offering-" + offeringId).toAbsolutePath().normalize();
            Files.createDirectories(directory); return Files.copy(source, directory.resolve(UUID.randomUUID() + extension));
        } catch (IOException failure) { throw new IllegalStateException("Unable to store material.", failure); }
    }
    private String extension(String path) { int index = path.lastIndexOf('.'); return index < 0 ? "file" : path.substring(index + 1); }
    private JPanel fields(Object... pairs) { JPanel panel = new JPanel(new GridLayout(pairs.length / 2, 2, 8, 8)); for (int i=0;i<pairs.length;i+=2) { panel.add(new JLabel((String)pairs[i])); panel.add((JTextField)pairs[i+1]); } return panel; }
    private void button(JPanel panel, String text, Runnable action) { JButton button = new JButton(text); button.addActionListener(event -> { try { action.run(); } catch (IllegalArgumentException | IllegalStateException invalid) { error(invalid.getMessage()); } }); panel.add(button); }
    private void show(ApiResponse<?> response) { if (response.successful()) JOptionPane.showMessageDialog(this, "Material saved. Publish it from the lecturer workflow when ready."); else error(response.error().message()); }
    private void error(String text) { JOptionPane.showMessageDialog(this, text, "Course materials", JOptionPane.ERROR_MESSAGE); }
}
