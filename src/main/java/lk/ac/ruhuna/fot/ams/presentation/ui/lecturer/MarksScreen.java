package lk.ac.ruhuna.fot.ams.presentation.ui.lecturer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController.SaveMarksRequest;
import lk.ac.ruhuna.fot.ams.business.service.MarksService.MarkInput;
import lk.ac.ruhuna.fot.ams.business.service.MarksService.MarkSheet;
import lk.ac.ruhuna.fot.ams.business.service.MarksService.StudentMarks;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ApiCall;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Dialogs;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ObjectTableModel;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Lecturer marks entry. The screen only collects scores and renders the sheet built by MarksService.
 * Range checks (0-100), ownership and CA contribution are all decided by the business layer.
 */
public final class MarksScreen extends JPanel {
    private static final int FIXED_COLUMNS = 2;

    private final AuthenticatedSession session;
    private final MarksController marks;

    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final SheetModel model = new SheetModel();
    private final JTable table = new JTable(model);
    private final JLabel tableState = new JLabel(" ");
    private final JLabel message = new JLabel(" ");
    private final AppButton saveButton = new AppButton("Save marks");
    private final AppButton reloadButton = new AppButton("Reload", true);

    private MarkSheet sheet;
    private boolean populating;

    public MarksScreen(AuthenticatedSession session, MarksController marks) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.marks = marks;
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
        JLabel title = new JLabel("Assessments & Marks");
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
            if (!populating && offeringBox.getSelectedItem() != null) {
                loadSheet();
            }
        });
        reloadButton.addActionListener(event -> loadSheet());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        JLabel label = new JLabel("Offering");
        label.setLabelFor(offeringBox);
        filters.add(label);
        filters.add(offeringBox);
        filters.add(reloadButton);
        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildTablePanel() {
        table.getAccessibleContext().setAccessibleName("Marks table");
        table.putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
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
        message.getAccessibleContext().setAccessibleName("Marks message");
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
        ApiCall.run(() -> marks.myOfferings(session), offerings -> {
            populating = true;
            offeringBox.removeAllItems();
            offerings.forEach(offeringBox::addItem);
            populating = false;
            if (offerings.isEmpty()) {
                tableState.setText("You have no assigned course offerings.");
            } else {
                loadSheet();
            }
        }, this::loadFailed);
    }

    private void loadSheet() {
        OfferingSummary selected = (OfferingSummary) offeringBox.getSelectedItem();
        if (selected == null) {
            return;
        }
        message.setText(" ");
        tableState.setForeground(UiTheme.MUTED);
        tableState.setText("Loading marks\u2026");
        saveButton.setEnabled(false);
        ApiCall.run(() -> marks.sheet(session, selected.offeringId()), loaded -> {
            sheet = loaded;
            model.load(loaded);
            if (loaded.assessments().isEmpty()) {
                tableState.setText("No assessment plan has been published for this offering.");
            } else if (loaded.students().isEmpty()) {
                tableState.setText("No active students are enrolled in this offering.");
            } else {
                tableState.setText(loaded.students().size() + " student(s). Enter marks from 0 to 100.");
                saveButton.setEnabled(true);
            }
        }, this::loadFailed);
    }

    private void loadFailed(ApiError error) {
        model.load(null);
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
        List<MarkInput> changes = model.collectChanges();
        if (changes == null) {
            message.setText("\u26A0 Some entries are not valid numbers. Fix the rows marked INVALID.");
            return;
        }
        if (changes.isEmpty()) {
            message.setForeground(UiTheme.MUTED);
            message.setText("No changes to save.");
            return;
        }
        if (!Dialogs.confirm(this, "Confirm save", "Save " + changes.size() + " mark(s)?")) {
            return;
        }
        message.setForeground(UiTheme.ERROR);
        saveButton.setEnabled(false);
        int offeringId = sheet.offeringId();
        ApiCall.run(() -> marks.saveMarks(session, new SaveMarksRequest(offeringId, changes)), saved -> {
            message.setForeground(UiTheme.SUCCESS);
            message.setText("\u2713 " + saved.savedCount() + " mark(s) saved.");
            loadSheet();
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

    /** Input buffer for the grid. Holds typed text and displays service-provided values; computes nothing. */
    private static final class SheetModel extends AbstractTableModel {
        private MarkSheet sheet;
        private String[][] original = new String[0][0];
        private String[][] current = new String[0][0];
        private boolean[] invalid = new boolean[0];

        void load(MarkSheet loaded) {
            sheet = loaded;
            int rows = loaded == null ? 0 : loaded.students().size();
            int columns = loaded == null ? 0 : loaded.assessments().size();
            original = new String[rows][columns];
            current = new String[rows][columns];
            invalid = new boolean[rows];
            for (int r = 0; r < rows; r++) {
                StudentMarks student = loaded.students().get(r);
                for (int c = 0; c < columns; c++) {
                    BigDecimal score = student.scores().get(loaded.assessments().get(c).id());
                    String text = score == null ? "" : score.stripTrailingZeros().toPlainString();
                    original[r][c] = text;
                    current[r][c] = text;
                }
            }
            fireTableStructureChanged();
        }

        /** Returns the changed entries, or null when any typed value is not a number (rows are flagged). */
        List<MarkInput> collectChanges() {
            List<MarkInput> inputs = new ArrayList<>();
            boolean allValid = true;
            for (int r = 0; r < current.length; r++) {
                invalid[r] = false;
                for (int c = 0; c < current[r].length; c++) {
                    String text = current[r][c] == null ? "" : current[r][c].trim();
                    if (text.isEmpty() || text.equals(original[r][c])) {
                        continue;
                    }
                    try {
                        inputs.add(new MarkInput(sheet.assessments().get(c).id(),
                                sheet.students().get(r).enrollmentId(), new BigDecimal(text)));
                    } catch (NumberFormatException notANumber) {
                        invalid[r] = true;
                        allValid = false;
                    }
                }
            }
            fireTableDataChanged();
            return allValid ? inputs : null;
        }

        private int assessmentCount() {
            return sheet == null ? 0 : sheet.assessments().size();
        }

        private String rowStatus(int row) {
            if (invalid[row]) {
                return "INVALID";
            }
            for (int c = 0; c < current[row].length; c++) {
                String text = current[row][c] == null ? "" : current[row][c].trim();
                if (!text.equals(original[row][c])) {
                    return "EDITED";
                }
            }
            return "SAVED";
        }

        @Override public int getRowCount() { return sheet == null ? 0 : sheet.students().size(); }

        @Override public int getColumnCount() {
            return sheet == null ? 0 : FIXED_COLUMNS + assessmentCount() + 2;
        }

        @Override public String getColumnName(int column) {
            if (column == 0) {
                return "Student no.";
            }
            if (column == 1) {
                return "Name";
            }
            int index = column - FIXED_COLUMNS;
            if (index < assessmentCount()) {
                AssessmentRow assessment = sheet.assessments().get(index);
                return assessment.name() + " (" + assessment.weight().stripTrailingZeros().toPlainString() + "%"
                        + (assessment.caComponent() ? ", CA" : "") + ")";
            }
            return index == assessmentCount() ? "CA contribution" : "Status";
        }

        @Override public Object getValueAt(int row, int column) {
            StudentMarks student = sheet.students().get(row);
            if (column == 0) {
                return student.studentNumber();
            }
            if (column == 1) {
                return student.fullName();
            }
            int index = column - FIXED_COLUMNS;
            if (index < assessmentCount()) {
                return current[row][index];
            }
            if (index == assessmentCount()) {
                return student.caContribution() == null ? "\u2013"
                        : student.caContribution().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
            }
            return rowStatus(row);
        }

        @Override public boolean isCellEditable(int row, int column) {
            int index = column - FIXED_COLUMNS;
            return index >= 0 && index < assessmentCount();
        }

        @Override public void setValueAt(Object value, int row, int column) {
            int index = column - FIXED_COLUMNS;
            if (index >= 0 && index < assessmentCount()) {
                current[row][index] = value == null ? "" : value.toString();
                invalid[row] = false;
                fireTableRowsUpdated(row, row);
            }
        }
    }
}
