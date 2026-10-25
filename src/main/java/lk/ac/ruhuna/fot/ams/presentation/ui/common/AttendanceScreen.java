package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AttendanceScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final AttendanceController attendance;
    private final CourseController courses;

    public AttendanceScreen(AuthenticatedSession actor, AttendanceController attendance, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.attendance = attendance;
        this.courses = courses;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Attendance</h2>Use active enrolment IDs for marking. Session creation "
                + "requires a timetable entry on the same weekday. Students can view their own percentage.</html>"),
                BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        if (actor.role() == Role.LECTURER || actor.role() == Role.TECHNICAL_OFFICER) {
            button(actions, "Record attendance session", this::recordSession);
            button(actions, "List offering sessions", this::listSessions);
            button(actions, "View active enrolment roster", this::listEnrollments);
        }
        if (actor.role() == Role.ADMIN || actor.role() == Role.TECHNICAL_OFFICER) {
            button(actions, "Revise attendance record", this::reviseRecord);
        }
        if (actor.role() == Role.ADMIN) {
            button(actions, "Delete attendance session", this::deleteSession);
        }
        if (actor.role() == Role.UNDERGRADUATE) {
            button(actions, "View my attendance for all courses", this::viewAllSummaries);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void recordSession() {
        JTextField offering = field();
        JTextField component = field();
        JTextField date = field(LocalDate.now().toString());
        JTextField timetableEntry = field();
        JTextArea marks = new JTextArea(8, 40);
        marks.setText("Enrollment ID=P\nEnrollment ID=A");
        JPanel panel = form("Offering ID", offering, "Component ID", component,
                "Session date yyyy-mm-dd", date, "Timetable entry ID", timetableEntry);
        panel.add(new JLabel("One active enrolment per line: ID=P or ID=A"));
        panel.add(new javax.swing.JScrollPane(marks));
        if (JOptionPane.showConfirmDialog(this, panel, "Record attendance",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        Map<Integer, Boolean> byEnrollment = parseMarks(marks.getText());
        SessionRow session = new SessionRow(0, integer(offering), integer(component),
                LocalDate.parse(date.getText().trim()), integer(timetableEntry), null, null);
        show(attendance.saveSession(actor, new AttendanceController.SaveAttendanceRequest(session, byEnrollment)));
    }

    private Map<Integer, Boolean> parseMarks(String text) {
        Map<Integer, Boolean> records = new HashMap<>();
        for (String line : text.lines().map(String::trim).filter(line -> !line.isEmpty()).toList()) {
            String[] entry = line.split("=", -1);
            if (entry.length != 2 || (!entry[1].trim().equalsIgnoreCase("P")
                    && !entry[1].trim().equalsIgnoreCase("A"))) {
                throw new IllegalArgumentException("Use one line per enrolment, formatted ID=P or ID=A.");
            }
            if (records.putIfAbsent(Integer.parseInt(entry[0].trim()),
                    entry[1].trim().equalsIgnoreCase("P")) != null) {
                throw new IllegalArgumentException("An enrolment ID was entered more than once.");
            }
        }
        return Map.copyOf(records);
    }

    private void listSessions() {
        JTextField offering = field();
        if (!confirm("List attendance sessions", new String[]{"Offering ID"}, offering)) return;
        ApiResponse<java.util.List<SessionRow>> response =
                attendance.sessionsForOffering(actor, integer(offering));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String output = response.data().stream().map(row -> String.format(
                "Session %d | component %d | %s | timetable entry %s",
                row.id(), row.componentId(), row.date(), row.timetableEntryId()))
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, output.isBlank() ? "No sessions recorded." : output);
    }

    private void listEnrollments() {
        JTextField offering = field();
        if (!confirm("Active enrolments", new String[]{"Offering ID"}, offering)) return;
        ApiResponse<java.util.List<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow>> response =
                attendance.activeEnrollments(actor, integer(offering));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String roster = response.data().stream().map(row -> String.format(
                "Enrollment %d | student %d | attempt %d", row.id(), row.studentId(), row.attemptNumber()))
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, roster.isBlank() ? "No active enrolments." : roster);
    }

    private void reviseRecord() {
        JTextField session = field();
        JTextField enrollment = field();
        JTextField present = field("true or false");
        if (!confirm("Revise attendance", new String[]{"Session ID", "Enrolment ID", "Present true/false"},
                session, enrollment, present)) return;
        String value = present.getText().trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Present must be true or false.");
        }
        show(attendance.reviseRecord(actor, new AttendanceController.ReviseAttendanceRequest(
                new AttendanceRow(0, integer(session), integer(enrollment), Boolean.parseBoolean(value)))));
    }

    private void deleteSession() {
        JTextField session = field();
        if (!confirm("Delete attendance session", new String[]{"Session ID"}, session)) return;
        if (JOptionPane.showConfirmDialog(this, "Delete this recording and its attendance records?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            show(attendance.deleteSession(actor, integer(session)));
        }
    }

    private void viewSummary() {
        JTextField offering = field();
        if (!confirm("Attendance summary", new String[]{"Enrolled offering ID"}, offering)) return;
        ApiResponse<AttendanceSummary> result =
                attendance.calculateForCurrentStudent(actor, integer(offering));
        if (!result.successful()) {
            error(result.error().message());
            return;
        }
        AttendanceSummary summary = result.data();
        StringBuilder text = new StringBuilder();
        summary.components().forEach((type, component) -> text.append(String.format(
                "%s: %s%% (%d/%d)%n", type, component.percentage(),
                component.attendedSessions(), component.eligibleScheduledSessions())));
        text.append(String.format("Combined: %s%% | Required: %s%% | Eligible: %s",
                summary.combined().percentage(), summary.threshold(), summary.eligible()));
        JOptionPane.showMessageDialog(this, text.toString(), "Attendance summary",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewAllSummaries() {
        ApiResponse<java.util.List<lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView>> offerings =
                courses.offeringViewsForActor(actor);
        if (!offerings.successful()) { error(offerings.error().message()); return; }
        JTextArea output = new JTextArea(14, 60);
        output.setEditable(false);
        for (var offering : offerings.data()) {
            ApiResponse<AttendanceSummary> result = attendance.calculateForCurrentStudent(actor, offering.offering().id());
            if (result.successful()) {
                AttendanceSummary summary = result.data();
                output.append(String.format("%s — %s: %s%% | %s%n", offering.course().code(),
                        offering.course().title(), summary.combined().percentage(),
                        summary.eligible() ? "Eligible" : "Below attendance requirement"));
            }
        }
        if (output.getText().isBlank()) output.setText("No attendance records are available for your enrolled courses yet.");
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(output), "My attendance",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel form(Object... labelFieldPairs) {
        JPanel panel = new JPanel(new GridLayout(labelFieldPairs.length / 2, 2, 8, 8));
        for (int i = 0; i < labelFieldPairs.length; i += 2) {
            panel.add(new JLabel((String) labelFieldPairs[i]));
            panel.add((java.awt.Component) labelFieldPairs[i + 1]);
        }
        return panel;
    }

    private boolean confirm(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }
        return JOptionPane.showConfirmDialog(this, panel, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private JTextField field() {
        return new JTextField(20);
    }

    private JTextField field(String value) {
        return new JTextField(value, 20);
    }

    private int integer(JTextField field) {
        return Integer.parseInt(field.getText().trim());
    }

    private void button(JPanel panel, String title, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(title);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (IllegalArgumentException | java.time.DateTimeException invalid) {
                error("Check the entered IDs, attendance marks, or date: " + invalid.getMessage());
            }
        });
        panel.add(button);
    }

    private void show(ApiResponse<?> response) {
        if (response.successful()) {
            JOptionPane.showMessageDialog(this, "Attendance action completed.");
        } else {
            error(response.error().message());
        }
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Attendance", JOptionPane.ERROR_MESSAGE);
    }
}
