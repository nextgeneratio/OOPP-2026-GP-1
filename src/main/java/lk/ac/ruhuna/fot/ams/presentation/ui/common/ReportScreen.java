package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Common report shell: choose a report and its filters, run it, preview it, then print or export it.
 * Which data a user may see is decided by the services; the shell only shows what comes back.
 */
public final class ReportScreen extends JPanel {
    private final AuthenticatedSession session;
    private final ReportRunner runner;

    private final JComboBox<ReportCategory> categoryBox = new JComboBox<>();
    private final JComboBox<OfferingSummary> offeringBox = new JComboBox<>();
    private final JComboBox<RosterRow> studentBox = new JComboBox<>();
    private final JPanel offeringPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
    private final JPanel studentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
    private final ReportTableModel model = new ReportTableModel();
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final AppButton runButton = new AppButton("Run report");
    private final AppButton exportButton = new AppButton("Export CSV", true);
    private final AppButton printButton = new AppButton("Print", true);

    private List<RosterRow> roster = List.of();
    private ReportResult current;
    private boolean populating;

    public ReportScreen(AuthenticatedSession session, UiServices services) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.runner = new ReportRunner(session, services);
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildFilters(), BorderLayout.NORTH);
        add(ObjectTableModel.styledScroll(table), BorderLayout.CENTER);
        status.setFont(UiTheme.helper());
        add(status, BorderLayout.SOUTH);

        ReportCategory.forRole(session.role()).forEach(categoryBox::addItem);
        categoryBox.addActionListener(event -> categoryChanged());
        exportButton.setEnabled(false);
        printButton.setEnabled(false);
        categoryChanged();
        if (needsOffering()) {
            loadOfferings();
        }
    }

    private JPanel buildFilters() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("Reports");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);

        Combos.describe(offeringBox, o -> o.courseCode() + " \u2013 " + o.courseTitle() + " — "
                + o.academicYear() + (o.semesterNumber() > 0 ? " Semester " + o.semesterNumber() : ""));
        Combos.describe(studentBox, s -> s.studentNumber() + " \u2013 " + s.fullName());
        categoryBox.getAccessibleContext().setAccessibleName("Report");
        offeringBox.getAccessibleContext().setAccessibleName("Course offering");
        studentBox.getAccessibleContext().setAccessibleName("Student");
        SelectionOption.enableSearch(offeringBox);
        SelectionOption.enableSearch(studentBox);
        offeringBox.addActionListener(event -> { if (!populating) loadRoster(); });

        offeringPanel.setOpaque(false);
        offeringPanel.add(new JLabel("Offering"));
        offeringPanel.add(offeringBox);
        studentPanel.setOpaque(false);
        studentPanel.add(new JLabel("Student"));
        studentPanel.add(studentBox);
        runButton.addActionListener(event -> run());
        exportButton.addActionListener(event -> exportCsv());
        printButton.addActionListener(event -> print());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_MD, UiTheme.SPACE_SM));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JPanel reportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        reportPanel.setOpaque(false);
        reportPanel.add(new JLabel("Report"));
        reportPanel.add(categoryBox);
        row.add(reportPanel);
        row.add(offeringPanel);
        row.add(studentPanel);
        row.add(runButton);
        row.add(exportButton);
        row.add(printButton);
        header.add(title);
        header.add(row);
        return header;
    }

    private ReportCategory category() {
        return (ReportCategory) categoryBox.getSelectedItem();
    }

    private boolean needsOffering() {
        return category() != null && category().needsOffering(session.role());
    }

    private void categoryChanged() {
        ReportCategory category = category();
        offeringPanel.setVisible(needsOffering());
        studentPanel.setVisible(category != null && category.needsStudent());
        current = null;
        model.clear();
        exportButton.setEnabled(false);
        printButton.setEnabled(false);
        say(category == null ? "No reports are available for your role."
                : "Choose the filters, then press Run report.", false);
        runButton.setEnabled(category != null);
        if (needsOffering() && offeringBox.getItemCount() == 0 && offeringPanel.isVisible() && roster.isEmpty()) {
            // offerings are loaded once at start-up; nothing more to do when switching between categories
        }
    }

    private void say(String text, boolean error) {
        status.setForeground(error ? UiTheme.ERROR : UiTheme.MUTED);
        status.setText(text == null || text.isBlank() ? " " : (error ? "\u26A0 " : "") + text);
    }

    private void loadOfferings() {
        say("Loading offerings\u2026", false);
        ApiCall.run(runner::offerings, offerings -> {
            fill(offeringBox, offerings);
            if (offerings.isEmpty()) {
                say("No course offerings are assigned to you.", false);
            } else {
                loadRoster();
            }
        }, this::failed);
    }

    private void loadRoster() {
        OfferingSummary offering = (OfferingSummary) offeringBox.getSelectedItem();
        roster = List.of();
        fill(studentBox, List.of());
        if (offering == null) {
            return;
        }
        ApiCall.run(() -> runner.roster(offering.offeringId()), students -> {
            roster = students;
            fill(studentBox, students);
            say(students.isEmpty() ? "No active students are enrolled in this offering."
                    : "Choose the filters, then press Run report.", false);
        }, this::failed);
    }

    private void run() {
        ReportCategory category = category();
        OfferingSummary offering = needsOffering() ? (OfferingSummary) offeringBox.getSelectedItem() : null;
        RosterRow student = category.needsStudent() ? (RosterRow) studentBox.getSelectedItem() : null;
        if (needsOffering() && offering == null) {
            say("Choose a course offering first.", true);
            return;
        }
        if (category.needsStudent() && student == null) {
            say("Choose a student first.", true);
            return;
        }
        ReportRunner.Selection selection = new ReportRunner.Selection(offering, student, roster);
        runButton.setEnabled(false);
        exportButton.setEnabled(false);
        printButton.setEnabled(false);
        model.clear();
        say("Running report\u2026", false);
        ApiCall.run(() -> runner.run(category, selection), result -> {
            runButton.setEnabled(true);
            current = result;
            model.setResult(result);
            exportButton.setEnabled(!result.isEmpty());
            printButton.setEnabled(!result.isEmpty());
            say(result.isEmpty() ? "No records were found for the selected filters."
                    : result.rows().size() + " row(s).", false);
        }, error -> {
            runButton.setEnabled(true);
            failed(error);
        });
    }

    private void exportCsv() {
        if (current == null || current.isEmpty()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export report");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(current.title().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        if (file.exists() && !Dialogs.confirm(this, "Replace file", file.getName() + " already exists. Replace it?")) {
            return;
        }
        try {
            Files.writeString(file.toPath(), "\uFEFF" + CsvWriter.toCsv(current), StandardCharsets.UTF_8);
            say("Exported " + current.rows().size() + " row(s) to " + file.getName() + ".", false);
        } catch (IOException unwritable) {
            Dialogs.showError(this, new ApiError("EXPORT_FAILED",
                    "The report could not be saved. Choose another location and try again."));
        }
    }

    private void print() {
        if (current == null || current.isEmpty()) {
            return;
        }
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(current.title()), new MessageFormat("Page {0}"));
        } catch (PrinterException unprintable) {
            Dialogs.showError(this, new ApiError("PRINT_FAILED",
                    "The report could not be printed. Check the printer and try again."));
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
