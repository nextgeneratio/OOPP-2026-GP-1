package lk.ac.ruhuna.fot.ams.presentation.ui.technical;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController.DecideMedicalRequest;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController.MedicalSearchRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ScreenLookupController;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ApiCall;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Combos;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Dialogs;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.FormField;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Formats;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ObjectTableModel;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Technical Officer medical records. Date, overlap and approval rules are enforced by MedicalService; errors show here. */
public final class MedicalScreen extends JPanel {
    private final AuthenticatedSession session;
    private final AttendanceController attendance;
    private final MedicalController medical;
    private final ScreenLookupController lookup;

    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final JComboBox<RosterRow> studentBox = new JComboBox<>();
    private final ObjectTableModel<MedicalRow> model = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Medical date", MedicalRow::medicalDate),
            ObjectTableModel.column("Start", MedicalRow::startDate),
            ObjectTableModel.column("End", MedicalRow::endDate),
            ObjectTableModel.column("Reason", MedicalRow::reason),
            ObjectTableModel.column("Evidence", MedicalRow::evidenceReference),
            ObjectTableModel.column("Status", r -> r.status().name()),
            ObjectTableModel.column("Counts for eligibility", r -> r.affectsEligibility() ? "Yes" : "No")));
    private final JTable table = new JTable(model);
    private final AppButton approveButton = new AppButton("Approve");
    private final AppButton rejectButton = new AppButton("Reject", true);

    private final JTextField dateField = new JTextField();
    private final JTextField startField = new JTextField();
    private final JTextField endField = new JTextField();
    private final JTextField reasonField = new JTextField();
    private final JTextField evidenceField = new JTextField();
    private final FormField dateRow = new FormField("Medical date (" + Formats.DATE_HINT + ")", dateField);
    private final FormField startRow = new FormField("Start date (" + Formats.DATE_HINT + ")", startField);
    private final FormField endRow = new FormField("End date (" + Formats.DATE_HINT + ")", endField);
    private final FormField reasonRow = new FormField("Reason", reasonField);
    private final JLabel message = new JLabel(" ");
    private final AppButton submitButton = new AppButton("Submit medical record");
    private boolean populating;

    public MedicalScreen(AuthenticatedSession session, AttendanceController attendance,
                         MedicalController medical, ScreenLookupController lookup) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.attendance = attendance;
        this.medical = medical;
        this.lookup = lookup;
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);
        syncButtons();
        loadOfferings();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Medical Management");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);
        Combos.describe(offeringBox, o -> o.courseCode() + " \u2013 " + o.courseTitle());
        Combos.describe(studentBox, s -> s.studentNumber() + " \u2013 " + s.fullName());
        offeringBox.getAccessibleContext().setAccessibleName("Course offering");
        studentBox.getAccessibleContext().setAccessibleName("Student");
        offeringBox.addActionListener(event -> { if (!populating) loadStudents(); });
        studentBox.addActionListener(event -> { if (!populating) loadRecords(); });
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        filters.add(new JLabel("Offering"));
        filters.add(offeringBox);
        filters.add(new JLabel("Student"));
        filters.add(studentBox);
        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildCenter() {
        table.getSelectionModel().addListSelectionListener(event -> { if (!event.getValueIsAdjusting()) syncButtons(); });
        approveButton.addActionListener(event -> decide(ApprovalStatus.APPROVED));
        rejectButton.addActionListener(event -> decide(ApprovalStatus.REJECTED));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        actions.setOpaque(false);
        actions.add(approveButton);
        actions.add(rejectButton);
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setOpaque(false);
        panel.add(ObjectTableModel.styledScroll(table), BorderLayout.CENTER);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildForm() {
        message.setFont(UiTheme.helper());
        message.getAccessibleContext().setAccessibleName("Medical message");
        submitButton.addActionListener(event -> submit());
        JPanel fields = new JPanel(new java.awt.GridLayout(1, 0, UiTheme.SPACE_MD, 0));
        fields.setOpaque(false);
        fields.add(dateRow);
        fields.add(startRow);
        fields.add(endRow);
        fields.add(reasonRow);
        fields.add(new FormField("Evidence reference", evidenceField));
        JPanel bottom = new JPanel(new BorderLayout(UiTheme.SPACE_MD, 0));
        bottom.setOpaque(false);
        bottom.add(message, BorderLayout.CENTER);
        bottom.add(submitButton, BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD)));
        JLabel heading = new JLabel("New medical record");
        heading.setFont(UiTheme.sectionTitle());
        panel.add(heading, BorderLayout.NORTH);
        panel.add(fields, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void say(String text, boolean error) {
        message.setForeground(error ? UiTheme.ERROR : UiTheme.MUTED);
        message.setText(text == null || text.isBlank() ? " " : (error ? "\u26A0 " : "") + text);
    }

    private void syncButtons() {
        MedicalRow selected = selectedRecord();
        boolean pending = selected != null && selected.status() == ApprovalStatus.PENDING;
        approveButton.setEnabled(pending);
        rejectButton.setEnabled(pending);
        submitButton.setEnabled(studentBox.getSelectedItem() != null);
    }

    private MedicalRow selectedRecord() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.rowAt(table.convertRowIndexToModel(row));
    }

    private void loadOfferings() {
        say("Loading offerings\u2026", false);
        ApiCall.run(() -> attendance.myOfferings(session), offerings -> {
            fill(offeringBox, offerings);
            if (offerings.isEmpty()) {
                say("No course offerings are assigned to your department.", false);
            } else {
                loadStudents();
            }
        }, this::failed);
    }

    private void loadStudents() {
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        model.setRows(List.of());
        syncButtons();
        if (offering == null) {
            return;
        }
        ApiCall.run(() -> lookup.medicalStudents(session, offering.offeringId()), students -> {
            fill(studentBox, students);
            say(students.isEmpty() ? "No active students are enrolled in this offering." : "Select a student to see their medical records.", false);
            if (!students.isEmpty()) {
                loadRecords();
            }
            syncButtons();
        }, this::failed);
    }

    private void loadRecords() {
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        RosterRow student = (RosterRow) studentBox.getSelectedItem();
        if (offering == null || student == null) {
            return;
        }
        ApiCall.run(() -> medical.findForStudent(session, new MedicalSearchRequest(student.studentId(), offering.offeringId())),
                records -> {
                    model.setRows(records);
                    say(records.isEmpty() ? "No medical records for this student in this offering." : records.size() + " record(s).", false);
                    syncButtons();
                }, this::failed);
    }

    private void decide(ApprovalStatus decision) {
        MedicalRow record = selectedRecord();
        if (record == null) {
            return;
        }
        String verb = decision == ApprovalStatus.APPROVED ? "approve" : "reject";
        if (!Dialogs.confirm(this, "Confirm " + verb, "Do you want to " + verb + " this medical record ("
                + record.startDate() + " to " + record.endDate() + ")?\nA decided record cannot be changed.")) {
            return;
        }
        ApiCall.run(() -> medical.decide(session, new DecideMedicalRequest(record.id(), decision)), ignored -> {
            say("Medical record " + (decision == ApprovalStatus.APPROVED ? "approved." : "rejected."), false);
            loadRecords();
        }, this::failed);
    }

    private void submit() {
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        RosterRow student = (RosterRow) studentBox.getSelectedItem();
        if (offering == null || student == null) {
            return;
        }
        for (FormField row : List.of(dateRow, startRow, endRow)) {
            row.clearError();
        }
        LocalDate medicalDate = parse(dateRow, dateField);
        LocalDate start = parse(startRow, startField);
        LocalDate end = parse(endRow, endField);
        if (medicalDate == null || start == null || end == null) {
            return;
        }
        String evidence = evidenceField.getText().isBlank() ? null : evidenceField.getText().trim();
        MedicalRow row = new MedicalRow(0, student.studentId(), offering.offeringId(), medicalDate, start, end,
                reasonField.getText().trim(), evidence, ApprovalStatus.PENDING, false);
        submitButton.setEnabled(false);
        ApiCall.run(() -> medical.submit(session, row), saved -> {
            reasonField.setText("");
            evidenceField.setText("");
            say("Medical record submitted for approval.", false);
            loadRecords();
        }, error -> {
            submitButton.setEnabled(true);
            failed(error);
        });
    }

    private LocalDate parse(FormField row, JTextField field) {
        try {
            return Formats.parseDate(field.getText());
        } catch (DateTimeParseException invalid) {
            row.setError("Use " + Formats.DATE_HINT + ".");
            return null;
        }
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
