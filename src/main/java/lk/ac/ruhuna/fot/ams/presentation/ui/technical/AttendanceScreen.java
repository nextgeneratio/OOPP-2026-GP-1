package lk.ac.ruhuna.fot.ams.presentation.ui.technical;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController.SaveAttendanceRequest;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService.RosterEntry;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService.SessionSetup;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ApiCall;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Dialogs;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.FormField;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ObjectTableModel;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Technical Officer attendance recording. The screen collects the session and present/absent choices;
 * AttendanceService decides which timetable entry is valid, who must be marked, and performs the save.
 */
public final class AttendanceScreen extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AuthenticatedSession session;
    private final AttendanceController attendance;

    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final JComboBox<EntryRow> entryBox = new JComboBox<>();
    private final JTextField dateField = new JTextField(LocalDate.now().format(DATE_FORMAT), 12);
    private final RosterModel model = new RosterModel();
    private final JTable table = new JTable(model);
    private final JLabel tableState = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final AppButton saveButton = new AppButton("Save attendance");
    private final AppButton reloadButton = new AppButton("Reload", true);

    private SessionSetup setup;
    private boolean populatingOfferings;
    private boolean populatingEntries;

    public AttendanceScreen(AuthenticatedSession session, AttendanceController attendance) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.attendance = attendance;
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        saveButton.setEnabled(false);
        loadOfferings();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Attendance");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);

        offeringBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof OfferingSummary o
                        ? o.courseCode() + " \u2013 " + o.courseTitle() + " (semester " + o.semesterId() + ")"
                        : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        offeringBox.getAccessibleContext().setAccessibleName("Course offering");
        offeringBox.addActionListener(event -> {
            if (!populatingOfferings && offeringBox.getSelectedItem() != null) {
                loadSetup();
            }
        });

        entryBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof EntryRow e
                        ? e.dayOfWeek() + " " + e.start() + "\u2013" + e.end() + " \u00b7 " + e.location()
                        : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        entryBox.getAccessibleContext().setAccessibleName("Timetable session");
        entryBox.addActionListener(event -> {
            if (!populatingEntries) {
                saveButton.setEnabled(setup != null && !setup.roster().isEmpty()
                        && entryBox.getSelectedItem() != null);
            }
        });

        reloadButton.addActionListener(event -> loadSetup());

        FormField dateRow = new FormField("Session date (YYYY-MM-DD)", dateField);
        dateField.getAccessibleContext().setAccessibleName("Session date");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        JLabel offeringLabel = new JLabel("Offering");
        offeringLabel.setLabelFor(offeringBox);
        JLabel entryLabel = new JLabel("Session");
        entryLabel.setLabelFor(entryBox);
        filters.add(offeringLabel);
        filters.add(offeringBox);
        filters.add(entryLabel);
        filters.add(entryBox);
        filters.add(dateRow);
        filters.add(reloadButton);
        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildTablePanel() {
        table.getAccessibleContext().setAccessibleName("Attendance roster");
        tableState.setFont(UiTheme.helper());
        tableState.setForeground(UiTheme.MUTED);
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setOpaque(false);
        panel.add(ObjectTableModel.styledScroll(table), BorderLayout.CENTER);
        panel.add(tableState, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildFooter() {
        message.setFont(UiTheme.helper());
        message.setForeground(UiTheme.ERROR);
        message.getAccessibleContext().setAccessibleName("Attendance message");
        saveButton.addActionListener(event -> save());
        JPanel footer = new JPanel(new BorderLayout(UiTheme.SPACE_MD, 0));
        footer.setOpaque(false);
        footer.add(message, BorderLayout.CENTER);
        footer.add(saveButton, BorderLayout.EAST);
        return footer;
    }

    private void loadOfferings() {
        tableState.setForeground(UiTheme.MUTED);
        tableState.setText("Loading offerings\u2026");
        ApiCall.run(() -> attendance.myOfferings(session), offerings -> {
            populatingOfferings = true;
            offeringBox.removeAllItems();
            offerings.forEach(offeringBox::addItem);
            populatingOfferings = false;
            if (offerings.isEmpty()) {
                tableState.setText("No current-semester offerings are assigned to your department.");
            } else {
                loadSetup();
            }
        }, this::loadFailed);
    }

    private void loadSetup() {
        OfferingSummary selected = (OfferingSummary) offeringBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        message.setText(" ");
        tableState.setForeground(UiTheme.MUTED);
        tableState.setText("Loading session details\u2026");
        saveButton.setEnabled(false);
        ApiCall.run(() -> attendance.sessionSetup(session, selected.offeringId()), loaded -> {
            setup = loaded;
            model.load(loaded.roster());
            populatingEntries = true;
            entryBox.removeAllItems();
            loaded.entries().forEach(entryBox::addItem);
            populatingEntries = false;
            if (loaded.entries().isEmpty()) {
                tableState.setText("No timetable entry exists for this offering yet.");
            } else if (loaded.roster().isEmpty()) {
                tableState.setText("No active students are enrolled in this offering.");
            } else {
                tableState.setText(loaded.roster().size() + " student(s). Mark present/absent for every row.");
                saveButton.setEnabled(true);
            }
        }, this::loadFailed);
    }

    private void loadFailed(ApiError error) {
        setup = null;
        model.load(List.of());
        tableState.setForeground(UiTheme.ERROR);
        tableState.setText("\u26A0 " + error.message());
        if (!Dialogs.isInline(error)) {
            Dialogs.showError(this, error);
        }
    }

    private void save() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        message.setText(" ");
        EntryRow entry = (EntryRow) entryBox.getSelectedItem();
        if (setup == null || entry == null) {
            message.setText("\u26A0 Select a timetable session first.");
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateField.getText().trim(), DATE_FORMAT);
        } catch (DateTimeParseException invalid) {
            message.setText("\u26A0 Enter the date as YYYY-MM-DD.");
            return;
        }
        Map<Integer, Boolean> attendanceByEnrollment = model.attendanceMap();
        if (!Dialogs.confirm(this, "Confirm save",
                "Save attendance for " + attendanceByEnrollment.size() + " student(s) on " + date + "?")) {
            return;
        }
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        SessionRow request = new SessionRow(0, offering.offeringId(), entry.componentId(), date, entry.id(), 0);
        message.setForeground(UiTheme.ERROR);
        saveButton.setEnabled(false);
        ApiCall.run(() -> attendance.saveSession(session, new SaveAttendanceRequest(request, attendanceByEnrollment)),
                saved -> {
                    message.setForeground(UiTheme.SUCCESS);
                    message.setText("\u2713 Attendance session saved.");
                    saveButton.setEnabled(true);
                }, error -> {
                    saveButton.setEnabled(true);
                    if (Dialogs.isInline(error)) {
                        message.setForeground(UiTheme.ERROR);
                        message.setText("\u26A0 " + error.message());
                    } else {
                        Dialogs.showError(this, error);
                    }
                });
    }

    /** Student no., name and a Present checkbox column. Presence defaults to present; the officer unmarks absences. */
    private static final class RosterModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Student no.", "Name", "Present"};
        private List<RosterEntry> roster = List.of();
        private final Map<Integer, Boolean> presence = new HashMap<>();

        void load(List<RosterEntry> loaded) {
            roster = loaded;
            presence.clear();
            loaded.forEach(entry -> presence.put(entry.enrollmentId(), Boolean.TRUE));
            fireTableDataChanged();
        }

        Map<Integer, Boolean> attendanceMap() {
            return Map.copyOf(presence);
        }

        @Override public int getRowCount() { return roster.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Class<?> getColumnClass(int column) { return column == 2 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int row, int column) { return column == 2; }

        @Override public Object getValueAt(int row, int column) {
            RosterEntry entry = roster.get(row);
            return switch (column) {
                case 0 -> entry.studentNumber();
                case 1 -> entry.fullName();
                default -> presence.getOrDefault(entry.enrollmentId(), Boolean.TRUE);
            };
        }

        @Override public void setValueAt(Object value, int row, int column) {
            if (column == 2 && value instanceof Boolean present) {
                presence.put(roster.get(row).enrollmentId(), present);
            }
        }
    }
}
