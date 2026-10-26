package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Read-only SGPA/CGPA view; calculations stay in GpaService. */
public final class GradesGpaScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final AttendanceController attendance;
    private final GpaController gpa;
    private final ResultController results;

    public GradesGpaScreen(AuthenticatedSession actor, CourseController courses,
            AttendanceController attendance, GpaController gpa, ResultController results) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        this.attendance = attendance;
        this.gpa = gpa;
        this.results = results;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Grades &amp; GPA</h2>GPA is derived from finalized results "
                + "and cannot be edited.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel();
        javax.swing.JButton calculate = new javax.swing.JButton("View calculated GPA");
        calculate.addActionListener(event -> calculate());
        javax.swing.JButton viewResults = new javax.swing.JButton("View finalized results");
        viewResults.addActionListener(event -> viewResults());
        actions.add(calculate);
        actions.add(viewResults);
        add(actions, BorderLayout.CENTER);
    }

    private void viewResults() {
        Selection selected = chooseStudent("View finalized results");
        if (!selected.complete()) return;
        var response = results.studentResults(actor, selected.enrollment().studentId());
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        if (response.data().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No finalized course results are available yet.",
                    "Finalized results", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String text = response.data().stream().map(result -> result.courseCode()
                + " | semester " + result.semesterId() + " | attempt " + result.attemptNumber()
                + " | grade point " + result.gradePoint() + " | credits " + result.credits())
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, text, "Finalized results", JOptionPane.INFORMATION_MESSAGE);
    }

    private Selection chooseStudent(String title) {
        var offers = courses.offeringViewsForActor(actor);
        if (!offers.successful()) {
            error(offers.error().message());
            return Selection.empty();
        }
        JComboBox<OfferingView> offeringBox = offeringBox(offers.data());
        JComboBox<EnrollmentRow> studentBox = new JComboBox<>();
        offeringBox.addActionListener(event -> loadStudents(studentBox,
                (OfferingView) offeringBox.getSelectedItem()));
        loadStudents(studentBox, (OfferingView) offeringBox.getSelectedItem());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course offering"));
        form.add(offeringBox);
        form.add(new JLabel("Student enrolment"));
        form.add(studentBox);
        if (JOptionPane.showConfirmDialog(this, form, title,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return Selection.empty();
        return new Selection((OfferingView) offeringBox.getSelectedItem(),
                (EnrollmentRow) studentBox.getSelectedItem());
    }

    private void calculate() {
        var offers = courses.offeringViewsForActor(actor);
        if (!offers.successful()) {
            error(offers.error().message());
            return;
        }
        JComboBox<OfferingView> offeringBox = offeringBox(offers.data());
        JComboBox<EnrollmentRow> studentBox = new JComboBox<>();
        offeringBox.addActionListener(event -> loadStudents(studentBox,
                (OfferingView) offeringBox.getSelectedItem()));
        loadStudents(studentBox, (OfferingView) offeringBox.getSelectedItem());
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course offering / semester"));
        form.add(offeringBox);
        form.add(new JLabel("Student enrolment"));
        form.add(studentBox);
        if (JOptionPane.showConfirmDialog(this, form, "View calculated GPA",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        OfferingView offering = (OfferingView) offeringBox.getSelectedItem();
        EnrollmentRow enrollment = (EnrollmentRow) studentBox.getSelectedItem();
        if (offering == null || enrollment == null) {
            error("Choose an active student enrolment.");
            return;
        }
        GpaController.GpaRequest request = new GpaController.GpaRequest(
                enrollment.studentId(), offering.offering().semesterId());
        var sgpa = gpa.calculateSgpa(actor, request);
        var cgpa = gpa.calculateCgpa(actor, request);
        if (!sgpa.successful()) {
            error(sgpa.error().message());
            return;
        }
        if (!cgpa.successful()) {
            error(cgpa.error().message());
            return;
        }
        JOptionPane.showMessageDialog(this, "SGPA: " + sgpa.data().value()
                + "\nCGPA: " + cgpa.data().value(), "Grades & GPA", JOptionPane.INFORMATION_MESSAGE);
    }

    private JComboBox<OfferingView> offeringBox(java.util.List<OfferingView> offerings) {
        JComboBox<OfferingView> box = new JComboBox<>(offerings.toArray(OfferingView[]::new));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof OfferingView offering) setText(offering.course().code() + " — "
                        + offering.course().title() + " (semester " + offering.offering().semesterId() + ")");
                return this;
            }
        });
        return box;
    }

    private void loadStudents(JComboBox<EnrollmentRow> box, OfferingView offering) {
        box.removeAllItems();
        if (offering == null) return;
        var response = attendance.activeEnrollments(actor, offering.offering().id());
        if (!response.successful()) return;
        response.data().forEach(box::addItem);
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof EnrollmentRow enrollment) {
                    setText("Student " + enrollment.studentId() + " — enrolment " + enrollment.id());
                }
                return this;
            }
        });
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Grades & GPA", JOptionPane.ERROR_MESSAGE);
    }

    private record Selection(OfferingView offering, EnrollmentRow enrollment) {
        static Selection empty() { return new Selection(null, null); }
        boolean complete() { return offering != null && enrollment != null; }
    }
}
