package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.ComponentRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Role-adaptive attendance workflows. IDs live only in selected domain objects. */
public final class AttendanceScreen extends JPanel {
    private record OfferingOption(int id, String label) { @Override public String toString() { return label; } }
    private record StudentOption(int studentId, int enrollmentId, String label) {
        @Override public String toString() { return label; }
    }
    private record SessionOption(SessionRow session, String label) { @Override public String toString() { return label; } }

    private final AuthenticatedSession actor;
    private final AttendanceController attendance;
    private final CourseController courses;
    private final JComboBox<OfferingOption> offering = new JComboBox<>();
    private final JComboBox<ComponentRow> component = new JComboBox<>();
    private final JComboBox<StudentOption> student = new JComboBox<>();
    private final JComboBox<SessionOption> session = new JComboBox<>();
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private AttendanceService.SessionSetup setup;

    public AttendanceScreen(AuthenticatedSession actor, AttendanceController attendance, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.attendance = attendance;
        this.courses = courses;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Attendance</h2>Select a course and the relevant academic record.</html>"), BorderLayout.NORTH);
        add(selectors, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        loadOfferings();
        configureRole();
    }

    private void loadOfferings() {
        ApiResponse<List<OfferingView>> result = courses.offeringViewsForActor(actor);
        if (!result.successful()) { error(result.error().message()); return; }
        for (OfferingView view : result.data()) {
            offering.addItem(new OfferingOption(view.offering().id(),
                    view.course().code() + " — " + view.course().title()));
        }
        offering.addActionListener(event -> refreshSetup());
        if (offering.getItemCount() > 0) offering.setSelectedIndex(0);
        refreshSetup();
    }

    private void configureRole() {
        selectors.add(new JLabel("Course")); selectors.add(offering);
        switch (actor.role()) {
            case LECTURER -> {
                selectors.add(new JLabel("Component")); selectors.add(component);
                selectors.add(new JLabel("Student")); selectors.add(student);
                button("View attendance", this::viewLecturerAttendance);
                component.addActionListener(event -> refreshStudents());
            }
            case TECHNICAL_OFFICER -> {
                selectors.add(new JLabel("Component")); selectors.add(component);
                selectors.add(new JLabel("Session")); selectors.add(session);
                button("Record session", this::recordSession);
                button("View batch summary", this::viewBatchSummary);
                button("View selected session", this::loadSelectedSession);
                button("Save changes", this::saveChanges);
                component.addActionListener(event -> refreshSessions());
            }
            case ADMIN -> {
                selectors.add(new JLabel("Component")); selectors.add(component);
                selectors.add(new JLabel("Session")); selectors.add(session);
                button("View selected session", this::loadSelectedSession);
                button("Delete selected session", this::deleteSession);
                component.addActionListener(event -> refreshSessions());
            }
            case UNDERGRADUATE -> {
                selectors.add(new JLabel("Component")); selectors.add(component);
                button("View my attendance", this::viewMyAttendance);
            }
        }
        renderLabels();
    }

    private void refreshSetup() {
        OfferingOption selected = (OfferingOption) offering.getSelectedItem();
        setup = null;
        if (selected == null) return;
        ApiResponse<AttendanceService.SessionSetup> response = attendance.sessionSetup(actor, selected.id());
        if (!response.successful()) { error(response.error().message()); return; }
        setup = response.data();
        component.setModel(new DefaultComboBoxModel<>(setup.components().toArray(ComponentRow[]::new)));
        refreshStudents();
        refreshSessions();
    }

    private void refreshStudents() {
        if (setup == null) { student.setModel(new DefaultComboBoxModel<>()); return; }
        student.setModel(new DefaultComboBoxModel<>(setup.roster().stream()
                .map(row -> new StudentOption(row.studentId(), row.enrollmentId(),
                        row.studentNumber() + " — " + row.fullName())).toArray(StudentOption[]::new)));
    }

    private void refreshSessions() {
        if (setup == null) { session.setModel(new DefaultComboBoxModel<>()); return; }
        OfferingOption selected = (OfferingOption) offering.getSelectedItem();
        ComponentRow selectedComponent = (ComponentRow) component.getSelectedItem();
        if (selected == null || selectedComponent == null) { session.setModel(new DefaultComboBoxModel<>()); return; }
        ApiResponse<List<SessionRow>> response = attendance.sessionsForOffering(actor, selected.id());
        if (!response.successful()) { error(response.error().message()); return; }
        List<SessionRow> matching = response.data().stream().filter(row -> row.componentId() == selectedComponent.id()).toList();
        List<SessionOption> options = new java.util.ArrayList<>();
        for (int i = 0; i < matching.size(); i++) {
            SessionRow row = matching.get(i);
            EntryRow entry = setup.entries().stream().filter(value -> value.id() == row.timetableEntryId()).findFirst().orElse(null);
            String time = entry == null ? "" : " — " + entry.dayOfWeek() + " " + entry.start() + "–" + entry.end();
            String label = String.format("%s — Session %02d — %s%s", selectedComponent.type(), i + 1,
                    row.date().format(DateTimeFormatter.ISO_LOCAL_DATE), time);
            options.add(new SessionOption(row, label));
        }
        session.setModel(new DefaultComboBoxModel<>(options.toArray(SessionOption[]::new)));
    }

    private void viewLecturerAttendance() {
        StudentOption selectedStudent = (StudentOption) student.getSelectedItem();
        OfferingOption selectedOffering = (OfferingOption) offering.getSelectedItem();
        if (selectedStudent == null || selectedOffering == null) return;
        var response = attendance.calculateForEnrollment(actor,
                new AttendanceController.EnrollmentAttendanceRequest(selectedStudent.studentId(),
                        selectedStudent.enrollmentId(), selectedOffering.id()));
        if (!response.successful()) { error(response.error().message()); return; }
        showAttendanceDetail(response.data(), selectedStudent.enrollmentId(), selectedOffering.id(),
                (ComponentRow) component.getSelectedItem());
    }

    private void viewMyAttendance() {
        OfferingOption selected = (OfferingOption) offering.getSelectedItem();
        if (selected == null || setup == null || setup.roster().isEmpty()) return;
        var summary = attendance.calculateForCurrentStudent(actor, selected.id());
        if (!summary.successful()) { error(summary.error().message()); return; }
        showAttendanceDetail(summary.data(), setup.roster().get(0).enrollmentId(), selected.id(), null);
    }

    private void showAttendanceDetail(AttendanceSummary summary, int enrollmentId, int offeringId,
                                     ComponentRow selectedComponent) {
        StringBuilder text = new StringBuilder();
        summary.components().forEach((type, value) -> {
            if (selectedComponent == null || selectedComponent.type() == type)
                text.append(String.format("%s: %s%% (%d/%d, %d medically excused)%n",
                        type, value.percentage(), value.attendedSessions(), value.eligibleScheduledSessions(),
                        value.medicallyExcusedSessions()));
        });
        text.append(String.format("Combined: %s%% | Required: %s%% | %s", summary.combined().percentage(),
                summary.threshold(), summary.eligible() ? "Eligible" : "Below attendance requirement"));
        var history = attendance.historyForEnrollment(actor, enrollmentId, offeringId);
        if (!history.successful()) { error(history.error().message()); return; }
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Date", "Component", "Attendance"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        history.data().stream().filter(row -> selectedComponent == null || row.componentType() == selectedComponent.type())
                .forEach(row -> model.addRow(new Object[]{row.sessionDate(), row.componentType(),
                        row.present() ? "Present" : "Absent"}));
        JPanel detail = new JPanel(new BorderLayout(8, 8));
        detail.add(new JLabel("<html>" + text.toString().replace("\n", "<br>") + "</html>"), BorderLayout.NORTH);
        detail.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        detail.setPreferredSize(new java.awt.Dimension(600, 360));
        JOptionPane.showMessageDialog(this, detail, "Attendance summary and sessions", JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewBatchSummary() {
        if (setup == null) return;
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Student number", "Name", "Theory", "Practical", "Combined", "Eligibility"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        OfferingOption selected = (OfferingOption) offering.getSelectedItem();
        for (AttendanceService.RosterEntry row : setup.roster()) {
            var response = attendance.calculateForEnrollment(actor,
                    new AttendanceController.EnrollmentAttendanceRequest(row.studentId(), row.enrollmentId(), selected.id()));
            if (!response.successful()) { error(response.error().message()); return; }
            AttendanceSummary summary = response.data();
            String theory = summary.components().containsKey(lk.ac.ruhuna.fot.ams.domain.enums.ComponentType.THEORY)
                    ? summary.components().get(lk.ac.ruhuna.fot.ams.domain.enums.ComponentType.THEORY).percentage() + "%" : "—";
            String practical = summary.components().containsKey(lk.ac.ruhuna.fot.ams.domain.enums.ComponentType.PRACTICAL)
                    ? summary.components().get(lk.ac.ruhuna.fot.ams.domain.enums.ComponentType.PRACTICAL).percentage() + "%" : "—";
            model.addRow(new Object[]{row.studentNumber(), row.fullName(), theory, practical,
                    summary.combined().percentage() + "%", summary.eligible() ? "Eligible" : "Below threshold"});
        }
        JTable table = new JTable(model);
        JOptionPane.showMessageDialog(this, new JScrollPane(table), "Batch attendance summary", JOptionPane.PLAIN_MESSAGE);
    }

    private void recordSession() {
        ComponentRow selectedComponent = (ComponentRow) component.getSelectedItem();
        EntryRow entry = selectedEntry();
        if (setup == null || selectedComponent == null || entry == null) { error("Select a course, component, and timetable entry."); return; }
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField date = new JTextField(LocalDate.now().toString(), 12);
        JComboBox<EntryRow> timetable = new JComboBox<>(setup.entries().stream()
                .filter(value -> value.componentId() == selectedComponent.id()).toArray(EntryRow[]::new));
        timetable.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" :
                value.dayOfWeek() + " " + value.start() + "–" + value.end() + " — " + value.location()));
        form.add(new JLabel("Session date (yyyy-mm-dd)")); form.add(date);
        form.add(new JLabel("Timetable")); form.add(timetable);
        JTable marks = rosterTable(true);
        JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.add(form, BorderLayout.NORTH); panel.add(new JScrollPane(marks));
        panel.setPreferredSize(new java.awt.Dimension(720, 420));
        if (JOptionPane.showConfirmDialog(this, panel, "Record attendance", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        try {
            EntryRow selectedEntry = (EntryRow) timetable.getSelectedItem();
            if (selectedEntry == null) { error("No matching timetable entries are available."); return; }
            Map<Integer, Boolean> present = new HashMap<>();
            for (int row = 0; row < marks.getRowCount(); row++)
                present.put(setup.roster().get(row).enrollmentId(), (Boolean) marks.getValueAt(row, 2));
            SessionRow record = new SessionRow(0, currentOfferingId(), selectedComponent.id(),
                    LocalDate.parse(date.getText().trim()), selectedEntry.id(), null, null);
            var saved = attendance.saveSession(actor, new AttendanceController.SaveAttendanceRequest(record, present));
            if (!saved.successful()) error(saved.error().message()); else { info("Attendance session saved."); refreshSessions(); }
        } catch (RuntimeException invalid) { error("Check the session date: " + invalid.getMessage()); }
    }

    private void loadSelectedSession() {
        SessionOption selected = (SessionOption) session.getSelectedItem();
        if (selected == null || setup == null) return;
        var recordsResponse = attendance.recordsForSession(actor, selected.session().id());
        if (!recordsResponse.successful()) { error(recordsResponse.error().message()); return; }
        Map<Integer, Boolean> attendanceByEnrollment = new HashMap<>();
        recordsResponse.data().forEach(record -> attendanceByEnrollment.put(record.enrollmentId(), record.present()));
        JTable marks = rosterTable(false);
        for (int row = 0; row < setup.roster().size(); row++) {
            int enrollmentId = setup.roster().get(row).enrollmentId();
            marks.setValueAt(attendanceByEnrollment.getOrDefault(enrollmentId, false), row, 2);
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(marks), selected.label(), JOptionPane.PLAIN_MESSAGE);
    }

    private void saveChanges() {
        SessionOption selected = (SessionOption) session.getSelectedItem();
        if (selected == null || setup == null) return;
        var recordsResponse = attendance.recordsForSession(actor, selected.session().id());
        if (!recordsResponse.successful()) { error(recordsResponse.error().message()); return; }
        Map<Integer, Boolean> attendanceByEnrollment = new HashMap<>();
        recordsResponse.data().forEach(record -> attendanceByEnrollment.put(record.enrollmentId(), record.present()));
        JTable marks = rosterTable(true);
        for (int row = 0; row < setup.roster().size(); row++) {
            int enrollmentId = setup.roster().get(row).enrollmentId();
            marks.setValueAt(attendanceByEnrollment.getOrDefault(enrollmentId, false), row, 2);
        }
        if (JOptionPane.showConfirmDialog(this, new JScrollPane(marks), "Update attendance", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        Map<Integer, Boolean> updates = new HashMap<>();
        for (int row = 0; row < setup.roster().size(); row++)
            updates.put(setup.roster().get(row).enrollmentId(), (Boolean) marks.getValueAt(row, 2));
        var response = attendance.reviseSession(actor,
                new AttendanceController.ReviseSessionRequest(selected.session().id(), updates));
        if (!response.successful()) error(response.error().message()); else info("Attendance changes saved.");
    }

    private JTable rosterTable(boolean editable) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Student number", "Name", "Present"}, 0) {
            @Override public Class<?> getColumnClass(int column) { return column == 2 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int row, int column) { return editable && column == 2; }
        };
        if (setup != null) setup.roster().forEach(value -> model.addRow(new Object[]{
                value.studentNumber(), value.fullName(), false}));
        return new JTable(model);
    }

    private void deleteSession() {
        SessionOption selected = (SessionOption) session.getSelectedItem();
        if (selected == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete " + selected.label() + " and its attendance records?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            var response = attendance.deleteSession(actor, selected.session().id());
            if (!response.successful()) error(response.error().message()); else { info("Attendance session deleted."); refreshSessions(); }
        }
    }

    private EntryRow selectedEntry() { return setup == null ? null : setup.entries().stream()
            .filter(value -> component.getSelectedItem() != null && value.componentId() == ((ComponentRow) component.getSelectedItem()).id())
            .findFirst().orElse(null); }
    private int currentOfferingId() { return ((OfferingOption) offering.getSelectedItem()).id(); }
    private void renderLabels() {
        component.setRenderer((list, value, index, selected, focus) -> new JLabel(value == null ? "" : value.type().toString()));
    }
    private void button(String text, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(text);
        button.addActionListener(event -> { try { action.run(); } catch (RuntimeException failure) { error(failure.getMessage()); } });
        actions.add(button);
    }
    private void info(String text) { JOptionPane.showMessageDialog(this, text); }
    private void error(String text) { JOptionPane.showMessageDialog(this, text, "Attendance", JOptionPane.ERROR_MESSAGE); }
}
