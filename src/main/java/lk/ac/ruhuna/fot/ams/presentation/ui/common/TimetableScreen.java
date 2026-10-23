package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class TimetableScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final TimetableController timetables;

    public TimetableScreen(AuthenticatedSession actor, TimetableController timetables) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.timetables = timetables;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Timetable</h2>Timetables are separated by department, level, and semester. "
                + "Enter IDs from course management.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        addButton(actions, "View department / semester", this::view);
        addButton(actions, "Create timetable", this::createTimetable);
        addButton(actions, "Add timetable entry", this::addEntry);
        addButton(actions, "Update timetable entry", this::updateEntry);
        addButton(actions, "Remove timetable", this::deleteTimetable);
        add(actions, BorderLayout.CENTER);
    }

    private void view() {
        JTextField department = field();
        JTextField semester = field();
        JTextField level = field();
        JTextField day = field();
        if (!form("View timetable", new String[]{"Department ID", "Semester ID", "Level 1–4",
                "Day (optional MON-SUN)"}, department, semester, level, day)) return;
        ApiResponse<java.util.List<EntryRow>> response = timetables.listEntries(
                actor, integer(department), integer(semester), integer(level),
                day.getText().isBlank() ? null : day.getText().trim().toUpperCase(java.util.Locale.ROOT));
        if (!response.successful()) {
            showError(response.error().message());
            return;
        }
        JTextArea output = new JTextArea(16, 70);
        output.setEditable(false);
        response.data().forEach(entry -> output.append(String.format(
                "Entry %d | timetable %d | offering %d | component %d | %s %s-%s | %s%n",
                entry.id(), entry.timetableId(), entry.offeringId(), entry.componentId(),
                entry.dayOfWeek(), entry.start(), entry.end(), entry.location())));
        JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(output), "Timetable",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void createTimetable() {
        JTextField title = field();
        JTextField department = field();
        JTextField semester = field();
        JTextField level = field();
        if (!form("Create timetable", new String[]{"Title", "Department ID", "Semester ID", "Level 1–4"},
                title, department, semester, level)) return;
        show(timetables.create(actor, new TimetableRow(
                0, title.getText().trim(), integer(department), integer(semester), integer(level))));
    }

    private void addEntry() {
        showEntryForm(false);
    }

    private void updateEntry() {
        showEntryForm(true);
    }

    private void showEntryForm(boolean update) {
        JTextField entryId = field();
        JTextField timetableId = field();
        JTextField offeringId = field();
        JTextField componentId = field();
        JComboBox<String> day = new JComboBox<>(new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"});
        JTextField start = field();
        JTextField end = field();
        JTextField location = field();
        String[] labels = update
                ? new String[]{"Entry ID", "Timetable ID", "Offering ID", "Component ID",
                    "Day", "Start HH:mm", "End HH:mm", "Location"}
                : new String[]{"Timetable ID", "Offering ID", "Component ID",
                    "Day", "Start HH:mm", "End HH:mm", "Location"};
        java.awt.Component[] fields = update
                ? new java.awt.Component[]{entryId, timetableId, offeringId, componentId, day, start, end, location}
                : new java.awt.Component[]{timetableId, offeringId, componentId, day, start, end, location};
        if (!form(update ? "Update timetable entry" : "Add timetable entry", labels, fields)) return;
        EntryRow row = new EntryRow(update ? integer(entryId) : 0, integer(timetableId), integer(offeringId),
                integer(componentId), (String) day.getSelectedItem(),
                LocalTime.parse(start.getText().trim()), LocalTime.parse(end.getText().trim()),
                location.getText().trim());
        show(update ? timetables.updateEntry(actor, new TimetableController.EntryRequest(row))
                : timetables.addEntry(actor, new TimetableController.EntryRequest(row)));
    }

    private void deleteTimetable() {
        JTextField id = field();
        if (!form("Remove timetable", new String[]{"Timetable ID"}, id)) return;
        if (JOptionPane.showConfirmDialog(this, "Remove this timetable and all its entries?",
                "Confirm removal", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            show(timetables.delete(actor, integer(id)));
        }
    }

    private void addButton(JPanel panel, String label, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(label);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (NumberFormatException | java.time.DateTimeException invalid) {
                showError("Check the numeric IDs and time format (HH:mm).");
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

    private JTextField field() {
        return new JTextField(18);
    }

    private int integer(JTextField field) {
        return Integer.parseInt(field.getText().trim());
    }

    private void show(ApiResponse<?> response) {
        if (response.successful()) {
            JOptionPane.showMessageDialog(this, "Action completed successfully.", "Timetable",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            showError(response.error().message());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Timetable", JOptionPane.ERROR_MESSAGE);
    }
}
