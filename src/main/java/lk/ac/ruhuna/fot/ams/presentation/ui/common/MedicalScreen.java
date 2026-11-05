package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Medical request screen. Students submit their own PDF proof; reviewers decide scoped requests. */
public final class MedicalScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final MedicalController medical;
    private final CourseController courses;

    public MedicalScreen(AuthenticatedSession actor, MedicalController medical, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.medical = medical;
        this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Medical records</h2>Approved medicals are considered when attendance eligibility is calculated.</html>"),
                BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        addButton(actions, actor.role() == Role.UNDERGRADUATE ? "View my requests" : "View student requests", this::viewRecords);
        if (actor.role() == Role.UNDERGRADUATE) {
            addButton(actions, "Submit medical request (PDF proof)", this::recordMedical);
        }
        if (actor.role() == Role.TECHNICAL_OFFICER || actor.role() == Role.ADMIN) {
            addButton(actions, "View pending review queue", this::viewPending);
            addButton(actions, "Open submitted PDF proof", this::openProof);
            addButton(actions, "Approve or reject medical", this::decideMedical);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void viewRecords() {
        if (actor.role() == Role.UNDERGRADUATE) {
            showRecords(medical.findForCurrentStudent(actor));
            return;
        }
        JComboBox<SelectionOption> offering = offeringSelector();
        if (!form("Select offering", new String[]{"Course offering"}, offering)) return;
        int offeringId=selectedId(offering);
        var roster=courses.rosterForOffering(actor,offeringId);
        if(!roster.successful()){error(roster.error().message());return;}
        JComboBox<SelectionOption> student=options(roster.data().stream().map(row->new SelectionOption(row.studentId(),
                row.studentNumber()+" — "+row.fullName())).toList());
        if (!form("View medical records", new String[]{"Enrolled student"}, student)) return;
        ApiResponse<java.util.List<MedicalRow>> response = medical.findForStudent(actor,
                new MedicalController.MedicalSearchRequest(selectedId(student), offeringId));
        showRecords(response);
    }

    private void showRecords(ApiResponse<java.util.List<MedicalRow>> response) {
        if (!response.successful()) { error(response.error().message()); return; }
        JTextArea output = new JTextArea(14, 70);
        output.setEditable(false);
        response.data().forEach(row -> output.append(String.format(
                "Medical request | %s | %s to %s | %s%n%s%s%n%n",
                row.offeringId() == null ? "all courses" : offeringLabel(row.offeringId()), row.startDate(), row.endDate(),
                row.status(), row.reason(), row.status() == ApprovalStatus.REJECTED
                        ? "\nDecision reason: " + (row.rejectionReason() == null || row.rejectionReason().isBlank()
                                ? "No reason recorded (legacy request)." : row.rejectionReason()) : "")));
        if (response.data().isEmpty()) output.setText("No medical records match this student.");
        JOptionPane.showMessageDialog(this, new JScrollPane(output), "Medical records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void recordMedical() {
        var offeringResponse = courses.offeringViewsForActor(actor);
        if (!offeringResponse.successful()) { error(offeringResponse.error().message()); return; }
        OfferingChecklist offerings = new OfferingChecklist(offeringResponse.data().stream()
                .map(view -> new SelectionOption(view.offering().id(), view.course().code() + " — "
                        + view.course().title()))
                .toList());
        if (offerings.isEmpty()) { error("There are no course offerings available for your account."); return; }
        DatePickerField medicalDate = new DatePickerField(LocalDate.now(), false);
        DatePickerField start = new DatePickerField(LocalDate.now(), true);
        DatePickerField end = new DatePickerField(LocalDate.now(), true);
        JTextArea reason = new JTextArea(4, 25);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        if (!medicalRequestForm(offerings, medicalDate, start, end, reason)) return;
        java.util.List<Integer> selectedOfferingIds = offerings.selectedIds();
        if (selectedOfferingIds.isEmpty()) { error("Tick at least one course offering."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a doctor's PDF certificate (maximum 10 MB)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        ApiResponse<MedicalController.MedicalSubmissionResponse> response =
                medical.submitPdfForOfferings(actor, new MedicalRow(0, 0, null,
                medicalDate.value(), start.value(), end.value(), reason.getText().trim(), "pending",
                ApprovalStatus.PENDING, false, null), selectedOfferingIds, chooser.getSelectedFile().toPath());
        if (response.successful()) {
            MedicalController.MedicalSubmissionResponse result = response.data();
            if (result.status() == ApprovalStatus.REJECTED) {
                JOptionPane.showMessageDialog(this, result.submittedCount()
                        + " medical request(s) rejected. " + result.rejectionReason(),
                        "Medical request rejected", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Medical request submitted for " + result.submittedCount()
                        + " course offering(s) as pending approval.",
                        "Medical records", JOptionPane.INFORMATION_MESSAGE);
            }
        } else error(response.error().message());
    }

    private boolean medicalRequestForm(OfferingChecklist offerings, DatePickerField medicalDate,
            DatePickerField start, DatePickerField end, JTextArea reason) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(620, 430));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(7, 8, 7, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        addMedicalField(panel, c, "Course offerings", offerings);
        addMedicalField(panel, c, "Medical certificate date", medicalDate);
        addMedicalField(panel, c, "Absence starts", start);
        addMedicalField(panel, c, "Absence ends", end);
        c.gridx = 0; c.gridy++; c.weightx = 0; c.weighty = 0;
        panel.add(new JLabel("Reason"), c);
        c.gridx = 1; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        JScrollPane reasonScroll = new JScrollPane(reason);
        reasonScroll.setPreferredSize(new Dimension(390, 105));
        panel.add(reasonScroll, c);
        c.gridx = 0; c.gridy++; c.gridwidth = 2; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        JLabel note = new JLabel("The same dates, reason, and PDF will apply to every selected course.");
        note.setForeground(javax.swing.UIManager.getColor("Label.disabledForeground"));
        panel.add(note, c);
        return JOptionPane.showConfirmDialog(this, panel, "Submit medical request",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void addMedicalField(JPanel panel, GridBagConstraints c, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy++; c.gridwidth = 1; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        if (field instanceof OfferingChecklist checklist) checklist.setPreferredSize(new Dimension(390, 34));
        if (field instanceof JComboBox<?> combo) combo.setPreferredSize(new Dimension(390, 34));
        panel.add(field, c);
    }

    private static final class OfferingChecklist extends JPanel {
        private final java.util.List<SelectionOption> offerings;
        private final Set<Integer> selectedOfferingIds = new LinkedHashSet<>();
        private final JButton chooseButton = new JButton();

        private OfferingChecklist(java.util.List<SelectionOption> offerings) {
            super(new BorderLayout());
            this.offerings = offerings;
            chooseButton.addActionListener(event -> openChooser());
            add(chooseButton, BorderLayout.CENTER);
            updateButtonLabel();
        }

        private boolean isEmpty() { return offerings.isEmpty(); }

        private java.util.List<Integer> selectedIds() {
            return offerings.stream().map(SelectionOption::id).filter(selectedOfferingIds::contains).toList();
        }

        private void openChooser() {
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog dialog = new JDialog(owner, "Select course offerings", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel courseList = new JPanel(new GridLayout(0, 1, 0, 3));
            java.util.List<JCheckBox> checkBoxes = new java.util.ArrayList<>();
            for (SelectionOption offering : offerings) {
                JCheckBox checkBox = new JCheckBox(offering.label(), selectedOfferingIds.contains(offering.id()));
                checkBox.setToolTipText(offering.label());
                checkBoxes.add(checkBox);
                courseList.add(checkBox);
            }
            JScrollPane scroll = new JScrollPane(courseList,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(430, 245));

            JButton selectAll = new JButton("Select all");
            JButton clear = new JButton("Clear");
            selectAll.addActionListener(event -> checkBoxes.forEach(box -> box.setSelected(true)));
            clear.addActionListener(event -> checkBoxes.forEach(box -> box.setSelected(false)));
            JPanel bulkActions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
            bulkActions.add(selectAll);
            bulkActions.add(clear);

            JButton apply = new JButton("Apply selection");
            JButton cancel = new JButton("Cancel");
            apply.addActionListener(event -> {
                selectedOfferingIds.clear();
                for (int index = 0; index < offerings.size(); index++) {
                    if (checkBoxes.get(index).isSelected()) selectedOfferingIds.add(offerings.get(index).id());
                }
                updateButtonLabel();
                dialog.dispose();
            });
            cancel.addActionListener(event -> dialog.dispose());
            JPanel footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
            footer.add(cancel);
            footer.add(apply);

            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            content.add(bulkActions, BorderLayout.NORTH);
            content.add(scroll, BorderLayout.CENTER);
            content.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(content);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
        }

        private void updateButtonLabel() {
            int count = selectedOfferingIds.size();
            chooseButton.setText(count == 0 ? "Choose courses…"
                    : count + (count == 1 ? " course selected — Change" : " courses selected — Change"));
        }
    }

    private void decideMedical() {
        ApiResponse<java.util.List<MedicalRow>> pending=medical.pendingForReviewer(actor);
        if(!pending.successful()){error(pending.error().message());return;}
        JComboBox<SelectionOption> selected=options(pending.data().stream().map(row->new SelectionOption(row.id(),
                row.startDate()+" to "+row.endDate()+" — "+row.reason())).toList());
        JComboBox<ApprovalStatus> decision = new JComboBox<>(new ApprovalStatus[]{ApprovalStatus.APPROVED, ApprovalStatus.REJECTED});
        JTextArea reason = new JTextArea(3, 22);
        if (!form("Decide medical", new String[]{"Pending request", "Decision", "Reason (required for rejection)"},
                selected, decision, new JScrollPane(reason))) return;
        show(medical.decide(actor, new MedicalController.DecideMedicalRequest(selectedId(selected),
                (ApprovalStatus) decision.getSelectedItem(), reason.getText())), "Medical decision saved.");
    }

    private void viewPending() {
        showRecords(medical.pendingForReviewer(actor));
    }

    private void openProof() {
        ApiResponse<java.util.List<MedicalRow>> pending=medical.pendingForReviewer(actor);
        if(!pending.successful()){error(pending.error().message());return;}
        JComboBox<SelectionOption> selected=options(pending.data().stream().filter(row->row.evidenceReference()!=null)
                .map(row->new SelectionOption(row.id(),row.startDate()+" to "+row.endDate()+" — proof attached")).toList());
        if (!form("Open submitted PDF proof", new String[]{"Medical request"}, selected)) return;
        ApiResponse<java.nio.file.Path> response = medical.evidenceFor(actor, selectedId(selected));
        if (!response.successful()) { error(response.error().message()); return; }
        try {
            PdfViewerDialog.show(this, "Medical proof", response.data());
        } catch (java.io.IOException invalidPdf) {
            error("The stored medical proof could not be opened.");
        }
    }

    private boolean form(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel = new JPanel(new GridBagLayout());
        for (int i = 0; i < labels.length; i++) {
            java.awt.Component field = fields[i];
            if (field instanceof JComboBox<?> combo) {
                combo.setPreferredSize(new Dimension(360, combo.getPreferredSize().height));
            } else if (field instanceof JTextField text) {
                text.setPreferredSize(new Dimension(360, text.getPreferredSize().height));
            } else if (field instanceof JScrollPane fieldScroll) {
                fieldScroll.setPreferredSize(new Dimension(360, 100));
            }
            GridBagConstraints label = new GridBagConstraints();
            label.gridx = 0; label.gridy = i; label.anchor = GridBagConstraints.LINE_START;
            label.insets = new Insets(6, 4, 6, 12);
            panel.add(new JLabel(labels[i]), label);
            GridBagConstraints input = new GridBagConstraints();
            input.gridx = 1; input.gridy = i; input.weightx = 1; input.fill = GridBagConstraints.HORIZONTAL;
            input.insets = new Insets(6, 0, 6, 4);
            panel.add(field, input);
        }
        JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setPreferredSize(new java.awt.Dimension(620, Math.min(480, labels.length * 48 + 24)));
        return JOptionPane.showConfirmDialog(this, scroll, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static final class DatePickerField extends JPanel {
        private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy");
        private LocalDate selectedDate;
        private final JTextField display = new JTextField(18);
        private final boolean allowFuture;

        private DatePickerField(LocalDate initialDate, boolean allowFuture) {
            super(new BorderLayout(8, 0));
            this.allowFuture = allowFuture;
            setDate(initialDate);
            display.setEditable(false);
            display.setFocusable(false);
            JButton choose = new JButton("Choose date…");
            choose.addActionListener(event -> {
                Window parent = SwingUtilities.getWindowAncestor(this);
                CalendarDateDialog picker = new CalendarDateDialog(parent, selectedDate, allowFuture);
                LocalDate choice = picker.showPicker();
                if (choice != null) setDate(choice);
            });
            add(display, BorderLayout.CENTER);
            add(choose, BorderLayout.EAST);
        }

        private LocalDate value() { return selectedDate; }

        private void setDate(LocalDate date) {
            selectedDate = date;
            display.setText(date.format(DISPLAY_DATE));
        }
    }

    private static final class CalendarDateDialog {
        private final JDialog dialog;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel days = new JPanel(new GridLayout(0, 7, 4, 4));
        private YearMonth month;
        private LocalDate selection;
        private final boolean allowFuture;

        private CalendarDateDialog(Window owner, LocalDate initialDate, boolean allowFuture) {
            month = YearMonth.from(initialDate);
            selection = initialDate;
            this.allowFuture = allowFuture;
            dialog = new JDialog(owner, "Choose a date", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel content = new JPanel(new BorderLayout(8, 8));
            content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            JPanel navigation = new JPanel(new BorderLayout(8, 0));
            JButton previous = new JButton("‹");
            JButton next = new JButton("›");
            previous.addActionListener(event -> { month = month.minusMonths(1); refresh(); });
            next.addActionListener(event -> { month = month.plusMonths(1); refresh(); });
            navigation.add(previous, BorderLayout.WEST);
            navigation.add(monthLabel, BorderLayout.CENTER);
            navigation.add(next, BorderLayout.EAST);
            content.add(navigation, BorderLayout.NORTH);
            content.add(days, BorderLayout.CENTER);
            JButton today = new JButton("Today");
            today.addActionListener(event -> { selection = LocalDate.now(); dialog.dispose(); });
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(event -> { selection = null; dialog.dispose(); });
            footer.add(today);
            footer.add(cancel);
            content.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(content);
            refresh();
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.pack();
            dialog.setLocationRelativeTo(owner);
        }

        private LocalDate showPicker() {
            dialog.setVisible(true);
            return selection;
        }

        private void refresh() {
            monthLabel.setText(month.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                    java.util.Locale.getDefault()) + " " + month.getYear());
            days.removeAll();
            String[] weekdays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            for (String weekday : weekdays) {
                JLabel label = new JLabel(weekday, SwingConstants.CENTER);
                label.setPreferredSize(new Dimension(42, 24));
                days.add(label);
            }
            int offset = month.atDay(1).getDayOfWeek().getValue() - 1;
            for (int blank = 0; blank < offset; blank++) days.add(new JLabel(""));
            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                LocalDate date = month.atDay(day);
                JButton button = new JButton(Integer.toString(day));
                button.setMargin(new Insets(3, 3, 3, 3));
                button.setPreferredSize(new Dimension(42, 32));
                button.setEnabled(allowFuture || !date.isAfter(LocalDate.now()));
                button.addActionListener(event -> { selection = date; dialog.dispose(); });
                days.add(button);
            }
            while (days.getComponentCount() % 7 != 0) days.add(new JLabel(""));
            days.revalidate();
            days.repaint();
        }
    }
    private JTextField field() { return new JTextField(18); }
    private JComboBox<SelectionOption> offeringSelector(){
        var response=courses.offeringViewsForActor(actor);if(!response.successful()){error(response.error().message());return options(java.util.List.of());}
        java.util.ArrayList<SelectionOption> rows=new java.util.ArrayList<>();
        response.data().forEach(v->rows.add(new SelectionOption(v.offering().id(),v.course().code()+" — "+v.semesterLabel())));return options(rows);
    }
    private JComboBox<SelectionOption> options(java.util.List<SelectionOption> values){JComboBox<SelectionOption> box=new JComboBox<>();values.forEach(box::addItem);if(values.isEmpty())box.addItem(new SelectionOption(0,"No matching records"));SelectionOption.enableSearch(box);return box;}
    private int selectedId(JComboBox<SelectionOption> box){Object value=box.getSelectedItem();if(value instanceof SelectionOption o)return o.id();throw new IllegalArgumentException("Choose a record.");}
    private String offeringLabel(Integer id){return courses.offeringViewsForActor(actor).data().stream().filter(v->v.offering().id()==id).map(v->v.course().code()+" — "+v.course().title()).findFirst().orElse("Course offering");}
    private void addButton(JPanel panel, String text, Runnable action) { javax.swing.JButton button = new javax.swing.JButton(text); button.addActionListener(event -> { try { action.run(); } catch (IllegalArgumentException | java.time.DateTimeException invalid) { error("Check the selected records and dates (yyyy-MM-dd)."); } }); panel.add(button); }
    private void show(ApiResponse<?> response, String success) { if (response.successful()) JOptionPane.showMessageDialog(this, success, "Medical records", JOptionPane.INFORMATION_MESSAGE); else error(response.error().message()); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Medical records", JOptionPane.ERROR_MESSAGE); }
}
