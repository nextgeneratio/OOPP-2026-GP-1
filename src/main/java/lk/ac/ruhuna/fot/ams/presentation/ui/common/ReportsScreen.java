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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentMarksReport;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Generates only the reports exposed by {@link ReportController}. */
public final class ReportsScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final AttendanceController attendance;
    private final ReportController reports;
    private final DefaultTableModel tableModel = new DefaultTableModel();

    public ReportsScreen(AuthenticatedSession actor, CourseController courses,
            AttendanceController attendance, ReportController reports) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        this.attendance = attendance;
        this.reports = reports;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Reports</h2>Generate attendance and marks reports that "
                + "your role is authorized to view.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel();
        JButton batchAttendance = new JButton("Batch attendance");
        batchAttendance.addActionListener(event -> batchAttendance());
        JButton studentAttendance = new JButton("Student attendance");
        studentAttendance.addActionListener(event -> studentAttendance());
        JButton studentMarks = new JButton("Student marks");
        studentMarks.addActionListener(event -> studentMarks());
        actions.add(batchAttendance);
        actions.add(studentAttendance);
        if (actor.role() == Role.LECTURER) actions.add(studentMarks);
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(actions, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        tableModel.setColumnIdentifiers(new Object[] {"No report generated"});
    }

    private void batchAttendance() {
        Selection selected = chooseFilters(false);
        if (selected.offering() == null) return;
        var response = reports.batchCourseAttendance(actor,
                new ReportController.OfferingReportRequest(selected.offering().offering().id()));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        tableModel.setDataVector(new Object[0][0], new Object[] {"Student", "Enrolment", "Sessions", "Present"});
        response.data().forEach(report -> tableModel.addRow(new Object[] {report.studentId(),
                report.enrollmentId(), report.sessions().size(), report.sessions().stream()
                        .filter(session -> session.present()).count()}));
        showEmptyStateIfNeeded("No enrolments were found for this offering.");
    }

    private void studentAttendance() {
        Selection selected = chooseFilters(true);
        if (!selected.complete()) return;
        var response = reports.studentAttendance(actor, requestFor(selected));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        tableModel.setDataVector(new Object[0][0], new Object[] {"Date", "Component", "Present"});
        response.data().sessions().forEach(session -> tableModel.addRow(new Object[] {
                session.sessionDate(), session.componentType(), session.present() ? "Present" : "Absent"}));
        showEmptyStateIfNeeded("No attendance sessions were found for this student.");
    }

    private void studentMarks() {
        Selection selected = chooseFilters(true);
        if (!selected.complete()) return;
        var response = reports.studentMarks(actor, requestFor(selected));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        showMarks(response.data());
    }

    private ReportController.StudentReportRequest requestFor(Selection selected) {
        return new ReportController.StudentReportRequest(selected.enrollment().studentId(),
                selected.enrollment().id(), selected.offering().offering().id());
    }

    private void showMarks(StudentMarksReport report) {
        tableModel.setDataVector(new Object[0][0], new Object[] {"Assessment", "Type", "Weight", "CA", "Score"});
        report.assessments().forEach(assessment -> {
            String score = report.marks().stream().filter(mark -> mark.assessmentId() == assessment.id())
                    .map(mark -> mark.score().toPlainString()).findFirst().orElse("Not entered");
            tableModel.addRow(new Object[] {assessment.name(), assessment.type(), assessment.weight(),
                    assessment.caComponent() ? "Yes" : "No", score});
        });
        showEmptyStateIfNeeded("No assessment plan is available for this offering.");
    }

    private Selection chooseFilters(boolean studentRequired) {
        var offerings = courses.offeringViewsForActor(actor);
        if (!offerings.successful()) {
            error(offerings.error().message());
            return Selection.empty();
        }
        if (offerings.data().isEmpty()) {
            info("No reportable course offerings are available for your role.");
            return Selection.empty();
        }
        JComboBox<OfferingView> offeringBox = offeringBox(offerings.data());
        JComboBox<EnrollmentRow> studentBox = new JComboBox<>();
        offeringBox.addActionListener(event -> loadStudents(studentBox,
                (OfferingView) offeringBox.getSelectedItem()));
        if (studentRequired && !loadStudents(studentBox, (OfferingView) offeringBox.getSelectedItem())) {
            return Selection.empty();
        }
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Course offering"));
        form.add(offeringBox);
        if (studentRequired) {
            form.add(new JLabel("Student enrolment"));
            form.add(studentBox);
        }
        if (JOptionPane.showConfirmDialog(this, form, "Choose report filters",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return Selection.empty();
        return new Selection((OfferingView) offeringBox.getSelectedItem(),
                studentRequired ? (EnrollmentRow) studentBox.getSelectedItem() : null);
    }

    private JComboBox<OfferingView> offeringBox(List<OfferingView> offerings) {
        JComboBox<OfferingView> box = new JComboBox<>(offerings.toArray(OfferingView[]::new));
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof OfferingView offering) {
                    setText(offering.course().code() + " — " + offering.course().title());
                }
                return this;
            }
        });
        return box;
    }

    private boolean loadStudents(JComboBox<EnrollmentRow> box, OfferingView offering) {
        box.removeAllItems();
        if (offering == null) return false;
        var response = reports.enrollmentsForReport(actor, offering.offering().id());
        if (!response.successful()) {
            error(response.error().message());
            return false;
        }
        if (response.data().isEmpty()) {
            info("No enrolments are available for this report filter.");
            return false;
        }
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
        return true;
    }

    private void showEmptyStateIfNeeded(String message) {
        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[] {message});
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Reports", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Reports", JOptionPane.ERROR_MESSAGE);
    }

    private record Selection(OfferingView offering, EnrollmentRow enrollment) {
        static Selection empty() { return new Selection(null, null); }
        boolean complete() { return offering != null && enrollment != null; }
    }
}
