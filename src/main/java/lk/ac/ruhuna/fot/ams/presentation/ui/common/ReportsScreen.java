package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.OfferingReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.StudentReportRequest;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService.RosterEntry;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentMarksReport;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.MarkRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceHistoryRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Common report shell (SRS section 17): filter area, preview table, empty/loading/error states.
 * Every figure shown comes straight from ReportService through ReportController; this screen only
 * picks the filters and renders the rows it is given.
 */
public final class ReportsScreen extends JPanel {
    private enum ReportKind { STUDENT_ATTENDANCE, BATCH_ATTENDANCE, CA_FINAL_MARKS }

    private record AttendanceRowView(String studentNumber, String fullName, String sessionDate,
                                      String component, String status) {
    }

    private record MarksRowView(String assessment, String type, String weight, String score) {
    }

    private final AuthenticatedSession session;
    private final ReportController reports;
    private final EligibilityController eligibility;
    private final AttendanceController attendance;
    private final boolean lecturerMode;

    private final JComboBox<ReportKind> kindBox = new JComboBox<>();
    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final JComboBox<RosterRow> lecturerStudentBox = new JComboBox<>();
    private final JLabel offeringLabel = new JLabel("Offering");
    private final JLabel studentLabel = new JLabel("Student");
    private final AppButton runButton = new AppButton("Run report");
    private final JLabel status = new JLabel(" ");

    private final ObjectTableModel<AttendanceRowView> attendanceModel = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Student no.", AttendanceRowView::studentNumber),
            ObjectTableModel.column("Name", AttendanceRowView::fullName),
            ObjectTableModel.column("Session date", AttendanceRowView::sessionDate),
            ObjectTableModel.column("Component", AttendanceRowView::component),
            ObjectTableModel.column("Status", AttendanceRowView::status)));

    private final ObjectTableModel<MarksRowView> marksModel = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Assessment", MarksRowView::assessment),
            ObjectTableModel.column("Type", MarksRowView::type),
            ObjectTableModel.column("Weight %", MarksRowView::weight),
            ObjectTableModel.column("Score", MarksRowView::score)));

    private final JTable previewTable = new JTable();

    private boolean populating;

    public ReportsScreen(AuthenticatedSession session, ReportController reports,
                          EligibilityController eligibility, AttendanceController attendance) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.reports = reports;
        this.eligibility = eligibility;
        this.attendance = attendance;
        this.lecturerMode = session.role() == Role.LECTURER;
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildPreview(), BorderLayout.CENTER);

        if (session.role() == Role.ADMIN) {
            kindBox.setEnabled(false);
            runButton.setEnabled(false);
            say("No reports are available for your role in this release.", false);
        } else {
            setupKinds();
            loadOfferings();
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Reports");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);

        kindBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof ReportKind kind ? label(kind) : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        kindBox.getAccessibleContext().setAccessibleName("Report type");
        kindBox.addActionListener(event -> {
            if (!populating) {
                onKindSelected();
            }
        });

        offeringBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof OfferingSummary o
                        ? o.courseCode() + " \u2013 " + o.courseTitle() : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        offeringBox.getAccessibleContext().setAccessibleName("Course offering");
        offeringBox.addActionListener(event -> {
            if (!populating) {
                onOfferingSelected();
            }
        });

        lecturerStudentBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof RosterRow r ? r.studentNumber() + " \u2013 " + r.fullName() : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        lecturerStudentBox.getAccessibleContext().setAccessibleName("Student");
        lecturerStudentBox.addActionListener(event -> {
            if (!populating) {
                runButton.setEnabled(canRun());
            }
        });

        runButton.addActionListener(event -> run());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        JLabel kindLabel = new JLabel("Report");
        kindLabel.setLabelFor(kindBox);
        offeringLabel.setLabelFor(offeringBox);
        studentLabel.setLabelFor(lecturerStudentBox);
        filters.add(kindLabel);
        filters.add(kindBox);
        filters.add(offeringLabel);
        filters.add(offeringBox);
        filters.add(studentLabel);
        filters.add(lecturerStudentBox);
        filters.add(runButton);

        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildPreview() {
        status.setFont(UiTheme.helper());
        status.setForeground(UiTheme.MUTED);
        previewTable.getAccessibleContext().setAccessibleName("Report preview");
        previewTable.setModel(attendanceModel);
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setOpaque(false);
        panel.add(ObjectTableModel.styledScroll(previewTable), BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private void setupKinds() {
        populating = true;
        kindBox.removeAllItems();
        if (lecturerMode) {
            kindBox.addItem(ReportKind.STUDENT_ATTENDANCE);
            kindBox.addItem(ReportKind.BATCH_ATTENDANCE);
            kindBox.addItem(ReportKind.CA_FINAL_MARKS);
        } else {
            kindBox.addItem(ReportKind.BATCH_ATTENDANCE);
        }
        populating = false;
        onKindSelected();
    }

    private void onKindSelected() {
        ReportKind kind = (ReportKind) kindBox.getSelectedItem();
        boolean needsStudent = kind == ReportKind.STUDENT_ATTENDANCE || kind == ReportKind.CA_FINAL_MARKS;
        studentLabel.setVisible(lecturerMode && needsStudent);
        lecturerStudentBox.setVisible(lecturerMode && needsStudent);
        previewTable.setModel(kind == ReportKind.CA_FINAL_MARKS ? marksModel : attendanceModel);
        runButton.setEnabled(canRun());
    }

    private boolean canRun() {
        ReportKind kind = (ReportKind) kindBox.getSelectedItem();
        if (kind == null || offeringBox.getSelectedItem() == null) {
            return false;
        }
        boolean needsStudent = kind == ReportKind.STUDENT_ATTENDANCE || kind == ReportKind.CA_FINAL_MARKS;
        return !(lecturerMode && needsStudent) || lecturerStudentBox.getSelectedItem() != null;
    }

    private void loadOfferings() {
        say("Loading offerings\u2026", false);
        ApiCall.run(
                () -> lecturerMode ? eligibility.myOfferings(session) : attendance.myOfferings(session),
                offerings -> {
                    populating = true;
                    offeringBox.removeAllItems();
                    offerings.forEach(offeringBox::addItem);
                    populating = false;
                    say(offerings.isEmpty() ? "You have no assigned course offerings." : " ", false);
                    if (!offerings.isEmpty() && lecturerMode) {
                        onOfferingSelected();
                    }
                }, this::failed);
    }

    private void onOfferingSelected() {
        lecturerStudentBox.removeAllItems();
        runButton.setEnabled(false);
        OfferingSummary selected = (OfferingSummary) offeringBox.getSelectedItem();
        if (selected == null || !lecturerMode) {
            return;
        }
        say("Loading roster\u2026", false);
        ApiCall.run(() -> eligibility.roster(session, selected.offeringId()), roster -> {
            populating = true;
            roster.forEach(lecturerStudentBox::addItem);
            populating = false;
            say(roster.isEmpty() ? "No active students are enrolled in this offering." : " ", false);
            runButton.setEnabled(canRun());
        }, this::failed);
    }

    private void run() {
        ReportKind kind = (ReportKind) kindBox.getSelectedItem();
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        if (kind == null || offering == null) {
            return;
        }
        say("Running report\u2026", false);
        runButton.setEnabled(false);
        switch (kind) {
            case STUDENT_ATTENDANCE -> runStudentAttendance(offering);
            case BATCH_ATTENDANCE -> runBatchAttendance(offering);
            case CA_FINAL_MARKS -> runMarks(offering);
        }
    }

    private void runStudentAttendance(OfferingSummary offering) {
        RosterRow student = (RosterRow) lecturerStudentBox.getSelectedItem();
        if (student == null) {
            runButton.setEnabled(true);
            return;
        }
        ApiCall.run(
                () -> reports.studentAttendance(session, new StudentReportRequest(
                        student.studentId(), student.enrollmentId(), offering.offeringId())),
                report -> showAttendance(List.of(report), Map.of(student.enrollmentId(),
                        student.studentNumber() + "\u0001" + student.fullName())),
                this::reportFailed);
    }

    private void runBatchAttendance(OfferingSummary offering) {
        if (lecturerMode) {
            ApiCall.run(() -> eligibility.roster(session, offering.offeringId()), roster -> {
                Map<Integer, String> names = new HashMap<>();
                roster.forEach(r -> names.put(r.enrollmentId(), r.studentNumber() + "\u0001" + r.fullName()));
                loadBatchAttendance(offering, names);
            }, this::reportFailed);
        } else {
            ApiCall.run(() -> attendance.sessionSetup(session, offering.offeringId()), setup -> {
                Map<Integer, String> names = new HashMap<>();
                for (RosterEntry entry : setup.roster()) {
                    names.put(entry.enrollmentId(), entry.studentNumber() + "\u0001" + entry.fullName());
                }
                loadBatchAttendance(offering, names);
            }, this::reportFailed);
        }
    }

    private void loadBatchAttendance(OfferingSummary offering, Map<Integer, String> names) {
        ApiCall.run(() -> reports.batchCourseAttendance(session, new OfferingReportRequest(offering.offeringId())),
                batch -> showAttendance(batch, names), this::reportFailed);
    }

    private void runMarks(OfferingSummary offering) {
        RosterRow student = (RosterRow) lecturerStudentBox.getSelectedItem();
        if (student == null) {
            runButton.setEnabled(true);
            return;
        }
        ApiCall.run(
                () -> reports.studentMarks(session, new StudentReportRequest(
                        student.studentId(), student.enrollmentId(), offering.offeringId())),
                this::showMarks, this::reportFailed);
    }

    private void showAttendance(List<StudentAttendanceReport> batch, Map<Integer, String> names) {
        List<AttendanceRowView> rows = new ArrayList<>();
        for (StudentAttendanceReport report : batch) {
            String identity = names.getOrDefault(report.enrollmentId(), "\u0001Unknown student");
            String[] parts = identity.split("\u0001", 2);
            for (AttendanceHistoryRow session : report.sessions()) {
                rows.add(new AttendanceRowView(parts[0], parts[1], session.sessionDate().toString(),
                        session.componentType().toString(), session.present() ? "PRESENT" : "ABSENT"));
            }
        }
        previewTable.setModel(attendanceModel);
        attendanceModel.setRows(rows);
        runButton.setEnabled(true);
        say(rows.isEmpty() ? "No attendance sessions recorded yet." : rows.size() + " record(s).", false);
    }

    private void showMarks(StudentMarksReport report) {
        Map<Integer, MarkRow> byAssessment = new HashMap<>();
        report.marks().forEach(mark -> byAssessment.put(mark.assessmentId(), mark));
        List<MarksRowView> rows = new ArrayList<>();
        for (AssessmentRow assessment : report.assessments()) {
            MarkRow mark = byAssessment.get(assessment.id());
            rows.add(new MarksRowView(assessment.name(), assessment.type(),
                    assessment.weight().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    mark == null ? "\u2013" : mark.score().setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }
        previewTable.setModel(marksModel);
        marksModel.setRows(rows);
        runButton.setEnabled(true);
        say(rows.isEmpty() ? "No assessment plan has been published yet." : rows.size() + " assessment(s).", false);
    }

    private void reportFailed(ApiError error) {
        runButton.setEnabled(true);
        failed(error);
    }

    private void failed(ApiError error) {
        say(error.message(), true);
        if (!Dialogs.isInline(error)) {
            Dialogs.showError(this, error);
        }
    }

    private void say(String text, boolean error) {
        status.setForeground(error ? UiTheme.ERROR : UiTheme.MUTED);
        status.setText(text == null || text.isBlank() ? " " : (error ? "\u26A0 " : "") + text);
    }

    private static String label(ReportKind kind) {
        return switch (kind) {
            case STUDENT_ATTENDANCE -> "Student Attendance";
            case BATCH_ATTENDANCE -> "Batch/Course Attendance";
            case CA_FINAL_MARKS -> "CA / Final Marks";
        };
    }
}
