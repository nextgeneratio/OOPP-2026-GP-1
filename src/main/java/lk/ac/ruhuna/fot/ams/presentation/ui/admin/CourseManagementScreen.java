package lk.ac.ruhuna.fot.ams.presentation.ui.admin;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class CourseManagementScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;

    public CourseManagementScreen(AuthenticatedSession actor, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Course and programme management</h2>"
                + "Use database identifiers from your institution's current records. Course deactivation "
                + "is a safe, reversible-style soft removal; historical enrolments remain intact.</html>"),
                BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        addAction(actions, "Create department (ICT/BST/ET)", this::createDepartment);
        addAction(actions, "Create level 1–4 batch", this::createBatch);
        addAction(actions, "Create semester", this::createSemester);
        addAction(actions, "Create course", this::createCourse);
        addAction(actions, "Edit course details", this::editCourse);
        addAction(actions, "Create course offering", this::createOffering);
        addAction(actions, "Assign lecturer", this::assignLecturer);
        addAction(actions, "Browse course catalog / IDs", this::browseCatalog);
        addAction(actions, "Deactivate course", this::deactivateCourse);
        add(actions, BorderLayout.CENTER);
    }

    private void createDepartment() {
        JTextField code = field("ICT, BST, or ET");
        JTextField name = field("Department name");
        JTextField office = field("Office location (optional)");
        if (!form("Create department", new String[]{"Code", "Name", "Office"}, code, name, office)) return;
        show(courses.createDepartment(actor, new CourseDao.DepartmentRow(
                0, code.getText(), name.getText(), emptyToNull(office.getText()), true)));
    }

    private void createBatch() {
        JTextField department = field("Department ID");
        JTextField year = field("Intake year");
        JTextField programme = field("Programme");
        JTextField code = field("Batch code");
        JTextField level = field("Level 1–4");
        if (!form("Create batch", new String[]{"Department ID", "Intake year", "Programme", "Batch code", "Level"},
                department, year, programme, code, level)) return;
        show(courses.createBatch(actor, new CourseDao.BatchRow(0, integer(department), integer(year),
                programme.getText(), code.getText(), integer(level))));
    }

    private void createSemester() {
        JTextField year = field("Academic year (e.g. 2026/2027)");
        JTextField number = field("Semester number: 1 or 2");
        JTextField start = field("Start date yyyy-mm-dd");
        JTextField end = field("End date yyyy-mm-dd");
        if (!form("Create semester", new String[]{"Academic year", "Semester", "Start", "End"},
                year, number, start, end)) return;
        show(courses.createSemester(actor, new CourseDao.SemesterRow(0, year.getText(), integer(number),
                java.time.LocalDate.parse(start.getText()), java.time.LocalDate.parse(end.getText()))));
    }

    private void createCourse() {
        JTextField department = field("Department ID");
        JTextField code = field("Course code");
        JTextField title = field("Title");
        JTextField credits = field("Total credits");
        JComboBox<ComponentType> component = new JComboBox<>(ComponentType.values());
        JTextField componentCredits = field("Component credits");
        JTextField sessions = field("Planned sessions");
        if (!form("Create course", new String[]{"Department ID", "Code", "Title", "Total credits",
                "Component type", "Component credits", "Planned sessions"},
                department, code, title, credits, component, componentCredits, sessions)) return;
        ComponentType selectedType = (ComponentType) component.getSelectedItem();
        List<CourseDao.ComponentRow> components = List.of(new CourseDao.ComponentRow(
                0, 0, selectedType, decimal(componentCredits), integer(sessions)));
        CourseDao.CourseRow course = new CourseDao.CourseRow(
                0, integer(department), code.getText(), title.getText(), decimal(credits), true);
        show(courses.createCourse(actor, new CourseController.CreateCourseRequest(course, components, null)));
    }

    private void createOffering() {
        JTextField course = field("Course ID");
        JTextField batch = field("Batch ID (contains level)");
        JTextField semester = field("Semester ID");
        JTextField coordinator = field("Coordinator lecturer ID (optional)");
        JTextField lecturers = field("Additional lecturer IDs (comma-separated, optional)");
        JComboBox<String> status = new JComboBox<>(new String[]{"OPEN", "CLOSED"});
        if (!form("Create offering", new String[]{"Course ID", "Batch ID", "Semester ID", "Coordinator ID",
                "Additional lecturers", "Status"}, course, batch, semester, coordinator, lecturers, status)) return;
        List<Integer> additional = Arrays.stream(lecturers.getText().split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(Integer::parseInt).toList();
        Integer coordinatorId = coordinator.getText().isBlank() ? null : integer(coordinator);
        CourseService.OfferingSetup setup = new CourseService.OfferingSetup(
                integer(batch), integer(semester), coordinatorId, (String) status.getSelectedItem(), additional);
        show(courses.createOffering(actor, new CourseController.CreateOfferingRequest(integer(course), setup)));
    }

    private void assignLecturer() {
        JTextField offering = field("Offering ID");
        JTextField lecturer = field("Lecturer ID");
        if (!form("Assign lecturer", new String[]{"Offering ID", "Lecturer ID"}, offering, lecturer)) return;
        show(courses.assignLecturer(actor,
                new CourseController.LecturerAssignmentRequest(integer(offering), integer(lecturer))));
    }

    private void editCourse() {
        JTextField id = field("Course ID");
        JTextField department = field("Department ID");
        JTextField code = field("Course code");
        JTextField title = field("Title");
        JTextField credits = field("Total credits");
        if (!form("Edit course details", new String[]{"Course ID", "Department ID", "Course code",
                "Title", "Total credits"}, id, department, code, title, credits)) return;
        show(courses.updateCourse(actor, new CourseDao.CourseRow(integer(id), integer(department),
                code.getText().trim(), title.getText().trim(), decimal(credits), true)));
    }

    private void deactivateCourse() {
        JTextField course = field("Course ID");
        if (!form("Deactivate course", new String[]{"Course ID"}, course)) return;
        show(courses.deactivateCourse(actor, integer(course)));
    }

    private void browseCatalog() {
        var response = courses.catalog(actor);
        if (!response.successful()) {
            show(response);
            return;
        }
        var catalog = response.data();
        StringBuilder text = new StringBuilder("DEPARTMENTS\n");
        catalog.departments().forEach(row -> text.append(String.format(
                "%d | %s | %s%n", row.id(), row.code(), row.name())));
        text.append("\nBATCHES\n");
        catalog.batches().forEach(row -> text.append(String.format(
                "%d | %s | department %d | level %d | intake %d%n",
                row.id(), row.code(), row.departmentId(), row.level(), row.intakeYear())));
        text.append("\nCOURSES\n");
        catalog.courses().forEach(row -> text.append(String.format(
                "%d | %s | %s | department %d | %s%n",
                row.id(), row.code(), row.title(), row.departmentId(), row.active() ? "active" : "inactive")));
        text.append("\nSEMESTERS\n");
        catalog.semesters().forEach(row -> text.append(String.format(
                "%d | %s semester %d%n", row.id(), row.academicYear(), row.semesterNumber())));
        text.append("\nOFFERINGS\n");
        catalog.offerings().forEach(row -> text.append(String.format(
                "%d | course %d | batch %d | semester %d | %s%n",
                row.id(), row.courseId(), row.batchId(), row.semesterId(), row.status())));
        JTextArea output = new JTextArea(text.toString(), 24, 78);
        output.setEditable(false);
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(output),
                "Course catalog", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addAction(JPanel panel, String label, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(label);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (NumberFormatException | java.time.DateTimeException invalid) {
                JOptionPane.showMessageDialog(this, "Check the entered identifier, number, or date.",
                        "Invalid input", JOptionPane.WARNING_MESSAGE);
            }
        });
        panel.add(button);
    }

    private boolean form(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }
        return JOptionPane.showConfirmDialog(this, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private JTextField field(String hint) {
        JTextField field = new JTextField();
        field.setToolTipText(hint);
        return field;
    }

    private int integer(JTextField field) {
        return Integer.parseInt(field.getText().trim());
    }

    private double decimal(JTextField field) {
        return Double.parseDouble(field.getText().trim());
    }

    private String emptyToNull(String value) {
        return value.isBlank() ? null : value.trim();
    }

    private void show(ApiResponse<?> response) {
        JOptionPane.showMessageDialog(this, response.successful()
                ? "Action completed successfully."
                : response.error().message(), "Course management",
                response.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }
}
