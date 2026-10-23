package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController.GpaReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController.StudentScopeRequest;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.CourseResultView;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.GpaReport;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.SemesterOption;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Course results and GPA, read-only. Every figure (marks, grades, SGPA, CGPA, totals) is calculated and rounded
 * by GradesService; this screen only chooses what to ask for and displays the answer.
 */
public final class GradesGpaScreen extends JPanel {
    private static final String[] METRICS = {"Counted courses", "Credits", "Grade points (sum)",
            "Weighted total", "SGPA", "CGPA"};

    private final AuthenticatedSession session;
    private final GradesController grades;
    private final EligibilityController eligibility;
    private final boolean lecturerMode;

    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final JComboBox<RosterRow> studentBox = new JComboBox<>();
    private final AppButton loadButton = new AppButton("Show results");
    private final ObjectTableModel<CourseResultView> resultsModel = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Course", CourseResultView::courseCode),
            ObjectTableModel.column("Semester", CourseResultView::semester),
            ObjectTableModel.column("Attempt", CourseResultView::attempt),
            ObjectTableModel.column("Credits", r -> r.credits().toPlainString()),
            ObjectTableModel.column("Final mark", r -> r.finalMark() == null ? "-" : r.finalMark().toPlainString()),
            ObjectTableModel.column("Grade", CourseResultView::grade),
            ObjectTableModel.column("Grade point", r -> r.gradePoint().toPlainString())));
    private final JComboBox<SemesterOption> semesterBox = new JComboBox<>();
    private final Map<String, JLabel> metricValues = new LinkedHashMap<>();
    private final JLabel status = new JLabel(" ");

    private Integer studentId;
    private boolean populating;

    public GradesGpaScreen(AuthenticatedSession session, GradesController grades, EligibilityController eligibility) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.grades = grades;
        this.eligibility = eligibility;
        this.lecturerMode = session.role() == Role.LECTURER;
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Course Results", ObjectTableModel.styledScroll(new JTable(resultsModel)));
        tabs.addTab("GPA", buildGpaPanel());
        add(tabs, BorderLayout.CENTER);
        status.setFont(UiTheme.helper());
        add(status, BorderLayout.SOUTH);
        clearMetrics();
        if (lecturerMode) {
            loadButton.setEnabled(false);
            loadOfferings();
        } else {
            loadResults();
        }
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Grades & GPA");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);
        header.add(title);
        if (lecturerMode) {
            offeringBox.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                        boolean selected, boolean focus) {
                    Object shown = value instanceof OfferingSummary o ? o.courseCode() + " \u2013 " + o.courseTitle() : value;
                    return super.getListCellRendererComponent(list, shown, index, selected, focus);
                }
            });
            studentBox.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                        boolean selected, boolean focus) {
                    Object shown = value instanceof RosterRow r ? r.studentNumber() + " \u2013 " + r.fullName() : value;
                    return super.getListCellRendererComponent(list, shown, index, selected, focus);
                }
            });
            offeringBox.getAccessibleContext().setAccessibleName("Course offering");
            studentBox.getAccessibleContext().setAccessibleName("Student");
            offeringBox.addActionListener(event -> { if (!populating) loadRoster(); });
            studentBox.addActionListener(event -> { if (!populating) loadButton.setEnabled(studentBox.getSelectedItem() != null); });
            loadButton.addActionListener(event -> {
                RosterRow student = (RosterRow) studentBox.getSelectedItem();
                if (student != null) {
                    studentId = student.studentId();
                    loadResults();
                }
            });
            JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
            filters.setOpaque(false);
            filters.setAlignmentX(LEFT_ALIGNMENT);
            filters.add(new JLabel("Offering"));
            filters.add(offeringBox);
            filters.add(new JLabel("Student"));
            filters.add(studentBox);
            filters.add(loadButton);
            header.add(filters);
        }
        return header;
    }

    private JPanel buildGpaPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_MD));
        panel.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        semesterBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof SemesterOption s ? s.label() : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        semesterBox.getAccessibleContext().setAccessibleName("Semester");
        semesterBox.addActionListener(event -> { if (!populating) loadGpa(); });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        top.setOpaque(false);
        top.add(new JLabel("Semester"));
        top.add(semesterBox);
        JPanel grid = new JPanel(new GridLayout(0, 2, UiTheme.SPACE_MD, UiTheme.SPACE_SM));
        grid.setBackground(UiTheme.SURFACE);
        grid.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD)));
        for (String metric : METRICS) {
            JLabel name = new JLabel(metric);
            name.setFont(UiTheme.label());
            name.setForeground(UiTheme.MUTED);
            JLabel value = new JLabel("-");
            value.setFont(UiTheme.sectionTitle());
            value.getAccessibleContext().setAccessibleName(metric);
            metricValues.put(metric, value);
            grid.add(name);
            grid.add(value);
        }
        panel.add(top, BorderLayout.NORTH);
        panel.add(grid, BorderLayout.CENTER);
        return panel;
    }

    private void say(String text, boolean error) {
        status.setForeground(error ? UiTheme.ERROR : UiTheme.MUTED);
        status.setText(text == null || text.isBlank() ? " " : (error ? "\u26A0 " : "") + text);
    }

    private void clearMetrics() {
        metricValues.values().forEach(label -> label.setText("-"));
    }

    private void loadOfferings() {
        say("Loading offerings\u2026", false);
        ApiCall.run(() -> eligibility.myOfferings(session), offerings -> {
            fill(offeringBox, offerings);
            say(offerings.isEmpty() ? "No course offerings are assigned to you." : " ", false);
            if (!offerings.isEmpty()) {
                loadRoster();
            }
        }, this::failed);
    }

    private void loadRoster() {
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        loadButton.setEnabled(false);
        if (offering == null) {
            return;
        }
        ApiCall.run(() -> eligibility.roster(session, offering.offeringId()), roster -> {
            fill(studentBox, roster);
            loadButton.setEnabled(!roster.isEmpty());
            say(roster.isEmpty() ? "No active students are enrolled in this offering." : "Select a student, then Show results.", false);
        }, this::failed);
    }

    private void loadResults() {
        say("Loading results\u2026", false);
        resultsModel.setRows(List.of());
        clearMetrics();
        StudentScopeRequest request = new StudentScopeRequest(studentId);
        ApiCall.run(() -> grades.courseResults(session, request), results -> {
            resultsModel.setRows(results);
            if (results.isEmpty()) {
                fill(semesterBox, List.of());
                say("No completed course results yet.", false);
            } else {
                say(results.size() + " completed course result(s).", false);
                loadSemesters(request);
            }
        }, this::failed);
    }

    private void loadSemesters(StudentScopeRequest request) {
        ApiCall.run(() -> grades.semesters(session, request), semesters -> {
            fill(semesterBox, semesters);
            if (!semesters.isEmpty()) {
                semesterBox.setSelectedIndex(semesters.size() - 1);
                loadGpa();
            }
        }, this::failed);
    }

    private void loadGpa() {
        SemesterOption semester = (SemesterOption) semesterBox.getSelectedItem();
        if (semester == null) {
            clearMetrics();
            return;
        }
        GpaReportRequest request = new GpaReportRequest(studentId, semester.id());
        ApiCall.run(() -> grades.gpaReport(session, request), this::showReport, this::failed);
    }

    private void showReport(GpaReport report) {
        metricValues.get("Counted courses").setText(String.valueOf(report.countedCourses()));
        metricValues.get("Credits").setText(String.valueOf(report.credits()));
        metricValues.get("Grade points (sum)").setText(String.valueOf(report.gradePointTotal()));
        metricValues.get("Weighted total").setText(String.valueOf(report.weightedTotal()));
        metricValues.get("SGPA").setText(String.valueOf(report.sgpa()));
        metricValues.get("CGPA").setText(String.valueOf(report.cgpa()));
    }

    private <T> void fill(JComboBox<T> box, List<T> items) {
        populating = true;
        box.removeAllItems();
        items.forEach(box::addItem);
        populating = false;
    }

    private void failed(ApiError error) {
        say(error.message(), true);
        if (!Dialogs.isInline(error)) {
            Dialogs.showError(this, error);
        }
    }
}
