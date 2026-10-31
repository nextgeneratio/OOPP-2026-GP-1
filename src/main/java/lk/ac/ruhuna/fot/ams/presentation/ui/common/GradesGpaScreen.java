package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Read-only results and GPA view backed by finalized course-result records. */
public final class GradesGpaScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final GpaController gpa;
    private final ResultController results;

    public GradesGpaScreen(AuthenticatedSession actor, CourseController courses,
            lk.ac.ruhuna.fot.ams.api.controller.AttendanceController unusedAttendance,
            GpaController gpa, ResultController results) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        this.gpa = gpa;
        this.results = results;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Grades &amp; GPA</h2>Results and GPA are calculated "
                + "from finalized academic records and cannot be edited here.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel();
        JButton viewResults = new JButton("View finalized results");
        viewResults.setName("view-finalized-results");
        viewResults.addActionListener(event -> viewResults());
        JButton calculate = new JButton("View calculated GPA");
        calculate.setName("view-calculated-gpa");
        calculate.addActionListener(event -> calculate());
        actions.add(viewResults);
        actions.add(calculate);
        add(actions, BorderLayout.CENTER);
    }

    private void viewResults() {
        if (actor.role() == Role.UNDERGRADUATE) {
            showResults(results.currentStudentResults(actor));
            return;
        }
        LecturerSelection selected = selectStudentForLecturer("View finalized results");
        if (selected != null) {
            showResults(results.studentResultsForOffering(actor, selected.studentId(), selected.offeringId()));
        }
    }

    private void showResults(lk.ac.ruhuna.fot.ams.api.controller.ApiResponse<List<ResultController.StudentResultResponse>> response) {
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        if (response.data().isEmpty()) {
            info("No finalized course results are available yet.", "Finalized results");
            return;
        }
        var semesterResponse = courses.semestersForActor(actor);
        if (!semesterResponse.successful()) { error(semesterResponse.error().message()); return; }
        String text = response.data().stream().map(result -> result.courseCode()
                + " | " + semesterResponse.data().stream().filter(term -> term.id() == result.semesterId())
                        .map(term -> term.academicYear() + " — Semester " + term.semesterNumber())
                        .findFirst().orElse("Academic term") + " | attempt " + result.attemptNumber()
                + " | grade point " + result.gradePoint() + " | credits " + result.credits()
                + " | completed " + result.completedAt().toLocalDate())
                .collect(java.util.stream.Collectors.joining("\n"));
        info(text, "Finalized results");
    }

    private void calculate() {
        if (actor.role() == Role.UNDERGRADUATE) {
            calculateOwnGpa();
            return;
        }
        LecturerSelection selected = selectStudentForLecturer("View calculated GPA");
        if (selected == null) return;
        Integer semesterId = chooseSemester(results.studentResultsForOffering(
                actor, selected.studentId(), selected.offeringId()));
        if (semesterId == null) return;
        showGpa(gpa.calculateLecturerSgpa(actor, selected.studentId(), selected.offeringId(), semesterId),
                gpa.calculateLecturerCgpa(actor, selected.studentId(), selected.offeringId()));
    }

    private void calculateOwnGpa() {
        Integer semesterId = chooseSemester(results.currentStudentResults(actor));
        if (semesterId == null) return;
        showGpa(gpa.calculateOwnSgpa(actor, semesterId), gpa.calculateOwnCgpa(actor));
    }

    private Integer chooseSemester(
            lk.ac.ruhuna.fot.ams.api.controller.ApiResponse<List<ResultController.StudentResultResponse>> response) {
        if (!response.successful()) {
            error(response.error().message());
            return null;
        }
        List<Integer> semesterIds = response.data().stream().map(ResultController.StudentResultResponse::semesterId)
                .distinct().sorted().toList();
        if (semesterIds.isEmpty()) {
            info("No finalized results are available for GPA calculation.", "Grades & GPA");
            return null;
        }
        var semesterResponse = courses.semestersForActor(actor);
        if (!semesterResponse.successful()) { error(semesterResponse.error().message()); return null; }
        List<SelectionOption> semesterOptions = semesterResponse.data().stream()
                .filter(term -> semesterIds.contains(term.id()))
                .map(term -> new SelectionOption(term.id(), term.academicYear() + " — Semester " + term.semesterNumber())).toList();
        JComboBox<SelectionOption> semester = new JComboBox<>(semesterOptions.toArray(SelectionOption[]::new));
        SelectionOption.enableSearch(semester);
        if (JOptionPane.showConfirmDialog(this, panel("Semester", semester), "Choose semester",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        Object selected = semester.getSelectedItem();
        return selected instanceof SelectionOption option ? option.id() : null;
    }

    private void showGpa(lk.ac.ruhuna.fot.ams.api.controller.ApiResponse<GpaController.GpaResponse> sgpa,
            lk.ac.ruhuna.fot.ams.api.controller.ApiResponse<GpaController.GpaResponse> cgpa) {
        if (!sgpa.successful()) {
            error(sgpa.error().message());
            return;
        }
        if (!cgpa.successful()) {
            error(cgpa.error().message());
            return;
        }
        info("SGPA: " + sgpa.data().value() + "\nCGPA: " + cgpa.data().value(), "Grades & GPA");
    }

    private LecturerSelection selectStudentForLecturer(String title) {
        if (actor.role() != Role.LECTURER) return null;
        var offers = courses.offeringViewsForActor(actor);
        if (!offers.successful()) {
            error(offers.error().message());
            return null;
        }
        if (offers.data().isEmpty()) {
            info("No assigned course offerings are available.", title);
            return null;
        }
        JComboBox<OfferingView> offering = offeringBox(offers.data());
        JComboBox<StudentEnrollmentChoice> student = new JComboBox<>();
        offering.addActionListener(event -> loadResultEnrollments(student,
                (OfferingView) offering.getSelectedItem()));
        if (!loadResultEnrollments(student, (OfferingView) offering.getSelectedItem())) return null;
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course offering")); form.add(offering);
        form.add(new JLabel("Student enrolment")); form.add(student);
        if (JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return null;
        StudentEnrollmentChoice selected = (StudentEnrollmentChoice) student.getSelectedItem();
        if (selected == null) {
            error("Choose a student enrolment.");
            return null;
        }
        OfferingView selectedOffering = (OfferingView) offering.getSelectedItem();
        return new LecturerSelection(selected.enrollment().studentId(), selectedOffering.offering().id());
    }

    private boolean loadResultEnrollments(JComboBox<StudentEnrollmentChoice> box, OfferingView offering) {
        box.removeAllItems();
        if (offering == null) return false;
        var response = results.resultEnrollmentsForOffering(actor, offering.offering().id());
        if (!response.successful()) {
            error(response.error().message());
            return false;
        }
        if (response.data().isEmpty()) {
            info("This offering has no enrolments.", "Grades & GPA");
            return false;
        }
        var roster = courses.rosterForOffering(actor, offering.offering().id());
        if (!roster.successful()) { error(roster.error().message()); return false; }
        response.data().forEach(enrollment -> roster.data().stream()
                .filter(student -> student.enrollmentId() == enrollment.id())
                .findFirst().ifPresent(student -> box.addItem(new StudentEnrollmentChoice(enrollment,
                        student.studentNumber() + " — " + student.fullName()))));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof StudentEnrollmentChoice enrollment) setText(enrollment.label());
                return this;
            }
        });
        return true;
    }

    private JComboBox<OfferingView> offeringBox(List<OfferingView> offerings) {
        JComboBox<OfferingView> box = new JComboBox<>(offerings.toArray(OfferingView[]::new));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof OfferingView item) setText(item.course().code() + " — "
                        + item.course().title() + " — " + item.semesterLabel());
                return this;
            }
        });
        return box;
    }

    private JPanel panel(String label, java.awt.Component field) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 8));
        panel.add(new JLabel(label)); panel.add(field);
        return panel;
    }

    private void info(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Grades & GPA", JOptionPane.ERROR_MESSAGE);
    }

    private record LecturerSelection(int studentId, int offeringId) { }
    private record StudentEnrollmentChoice(EnrollmentRow enrollment, String label) {
        @Override public String toString() { return label; }
    }
}
