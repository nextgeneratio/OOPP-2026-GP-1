package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityRequest;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityResponse;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.OwnEligibilityRequest;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService.StudentOffering;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Final exam eligibility. Lecturers pick an offering and a student; undergraduates see their own
 * offerings only. All eligibility logic (CA threshold, attendance threshold, reasons) comes from
 * EligibilityService; this screen only collects the selection and renders the result text.
 */
public final class EligibilityScreen extends JPanel {
    private final AuthenticatedSession session;
    private final EligibilityController eligibility;
    private final boolean lecturerMode;

    private final JComboBox<Object> offeringBox = new JComboBox<>();
    private final JComboBox<RosterRow> studentBox = new JComboBox<>();
    private final JLabel studentLabel = new JLabel("Student");
    private final AppButton checkButton = new AppButton("Check eligibility");
    private final JTextArea result = new JTextArea();
    private final JLabel status = new JLabel(" ");

    private boolean populating;

    public EligibilityScreen(AuthenticatedSession session, EligibilityController eligibility) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.eligibility = eligibility;
        this.lecturerMode = session.role() == Role.LECTURER;
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildResultPanel(), BorderLayout.CENTER);
        checkButton.setEnabled(false);
        loadOfferings();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Final Exam Eligibility");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);

        offeringBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                String shown;
                if (value instanceof OfferingSummary o) {
                    shown = o.courseCode() + " \u2013 " + o.courseTitle();
                } else if (value instanceof StudentOffering o) {
                    shown = o.courseCode() + " \u2013 " + o.courseTitle();
                } else {
                    shown = String.valueOf(value);
                }
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        offeringBox.getAccessibleContext().setAccessibleName("Course offering");
        offeringBox.addActionListener(event -> {
            if (!populating) {
                onOfferingSelected();
            }
        });

        studentBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof RosterRow r ? r.studentNumber() + " \u2013 " + r.fullName() : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        studentBox.getAccessibleContext().setAccessibleName("Student");
        studentBox.addActionListener(event -> {
            if (!populating) {
                checkButton.setEnabled(studentBox.getSelectedItem() != null);
            }
        });

        checkButton.addActionListener(event -> check());

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        JLabel offeringLabel = new JLabel("Offering");
        offeringLabel.setLabelFor(offeringBox);
        filters.add(offeringLabel);
        filters.add(offeringBox);
        if (lecturerMode) {
            studentLabel.setLabelFor(studentBox);
            filters.add(studentLabel);
            filters.add(studentBox);
        }
        filters.add(checkButton);
        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildResultPanel() {
        status.setFont(UiTheme.helper());
        status.setForeground(UiTheme.MUTED);
        result.setEditable(false);
        result.setLineWrap(false);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        result.setBackground(UiTheme.SURFACE);
        result.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        result.getAccessibleContext().setAccessibleName("Eligibility result");
        JScrollPane scroll = new JScrollPane(result);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setOpaque(false);
        panel.add(status, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void loadOfferings() {
        status.setForeground(UiTheme.MUTED);
        status.setText("Loading offerings\u2026");
        if (lecturerMode) {
            ApiCall.run(() -> eligibility.myOfferings(session), offerings -> {
                populating = true;
                offeringBox.removeAllItems();
                offerings.forEach(offeringBox::addItem);
                populating = false;
                status.setText(offerings.isEmpty()
                        ? "You have no assigned course offerings." : " ");
                if (!offerings.isEmpty()) {
                    onOfferingSelected();
                }
            }, this::loadFailed);
        } else {
            ApiCall.run(() -> eligibility.myOfferingsAsStudent(session), offerings -> {
                populating = true;
                offeringBox.removeAllItems();
                offerings.forEach(offeringBox::addItem);
                populating = false;
                status.setText(offerings.isEmpty() ? "You have no active course enrolments." : " ");
                checkButton.setEnabled(!offerings.isEmpty());
            }, this::loadFailed);
        }
    }

    private void onOfferingSelected() {
        if (!lecturerMode) {
            return;
        }
        OfferingSummary selected = (OfferingSummary) offeringBox.getSelectedItem();
        studentBox.removeAllItems();
        checkButton.setEnabled(false);
        if (selected == null) {
            return;
        }
        status.setForeground(UiTheme.MUTED);
        status.setText("Loading roster\u2026");
        ApiCall.run(() -> eligibility.roster(session, selected.offeringId()), roster -> {
            populating = true;
            roster.forEach(studentBox::addItem);
            populating = false;
            status.setText(roster.isEmpty() ? "No active students are enrolled in this offering." : " ");
        }, this::loadFailed);
    }

    private void loadFailed(ApiError error) {
        status.setForeground(UiTheme.ERROR);
        status.setText("\u26A0 " + error.message());
        if (!Dialogs.isInline(error)) {
            Dialogs.showError(this, error);
        }
    }

    private void check() {
        checkButton.setEnabled(false);
        status.setForeground(UiTheme.MUTED);
        status.setText("Checking\u2026");
        result.setText("");
        if (lecturerMode) {
            OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
            RosterRow student = (RosterRow) studentBox.getSelectedItem();
            if (offering == null || student == null) {
                checkButton.setEnabled(true);
                return;
            }
            ApiCall.run(() -> eligibility.evaluate(session,
                            new EligibilityRequest(student.studentId(), student.enrollmentId(), offering.offeringId())),
                    response -> show(student.studentNumber() + " \u2013 " + student.fullName(), response),
                    this::checkFailed);
        } else {
            StudentOffering offering = (StudentOffering) offeringBox.getSelectedItem();
            if (offering == null) {
                checkButton.setEnabled(true);
                return;
            }
            ApiCall.run(() -> eligibility.evaluateOwn(session,
                            new OwnEligibilityRequest(offering.enrollmentId(), offering.offeringId())),
                    response -> show(null, response), this::checkFailed);
        }
    }

    private void checkFailed(ApiError error) {
        checkButton.setEnabled(true);
        status.setForeground(UiTheme.ERROR);
        status.setText("\u26A0 " + error.message());
        if (!Dialogs.isInline(error)) {
            Dialogs.showError(this, error);
        }
    }

    private void show(String studentLine, EligibilityResponse response) {
        checkButton.setEnabled(true);
        status.setForeground(UiTheme.MUTED);
        status.setText(" ");
        StringBuilder text = new StringBuilder();
        if (studentLine != null) {
            text.append(studentLine).append('\n').append('\n');
        }
        text.append("Final Exam Eligibility: ").append(response.eligible() ? "ELIGIBLE" : "NOT ELIGIBLE")
                .append("\n\n")
                .append(response.caEligible() ? "\u2713" : "\u2717").append(" CA Eligibility: ")
                .append(response.caEligible() ? "PASS" : "FAIL").append('\n')
                .append(response.attendanceEligible() ? "\u2713" : "\u2717").append(" Attendance Eligibility: ")
                .append(response.attendanceEligible() ? "PASS" : "FAIL");
        if (!response.failedConditions().isEmpty()) {
            text.append("\n\nReasons:");
            response.failedConditions().forEach(reason -> text.append("\n\u2717 ").append(reason));
        }
        result.setText(text.toString());
        result.setCaretPosition(0);
    }
}
