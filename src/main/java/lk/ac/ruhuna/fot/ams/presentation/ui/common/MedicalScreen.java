package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Medical request screen. Students submit their own PDF proof; reviewers decide scoped requests. */
public final class MedicalScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final MedicalController medical;

    public MedicalScreen(AuthenticatedSession actor, MedicalController medical) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.medical = medical;
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
        JTextField student = field();
        JTextField offering = new JTextField("optional");
        if (!form("View medical records", new String[]{"Student ID", "Offering ID (optional)"}, student, offering)) return;
        Integer offeringId = offering.getText().isBlank() || offering.getText().equals("optional")
                ? null : integer(offering);
        ApiResponse<java.util.List<MedicalRow>> response = medical.findForStudent(actor,
                new MedicalController.MedicalSearchRequest(integer(student), offeringId));
        showRecords(response);
    }

    private void showRecords(ApiResponse<java.util.List<MedicalRow>> response) {
        if (!response.successful()) { error(response.error().message()); return; }
        JTextArea output = new JTextArea(14, 70);
        output.setEditable(false);
        response.data().forEach(row -> output.append(String.format(
                "Medical %d | offering %s | %s to %s | %s%n%s%s%n%n",
                row.id(), row.offeringId() == null ? "all courses" : row.offeringId(), row.startDate(), row.endDate(),
                row.status(), row.reason(), row.status() == ApprovalStatus.REJECTED
                        ? "\nDecision reason: " + (row.rejectionReason() == null || row.rejectionReason().isBlank()
                                ? "No reason recorded (legacy request)." : row.rejectionReason()) : "")));
        if (response.data().isEmpty()) output.setText("No medical records match this student.");
        JOptionPane.showMessageDialog(this, new JScrollPane(output), "Medical records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void recordMedical() {
        JTextField offering = new JTextField("optional");
        JTextField medicalDate = new JTextField(LocalDate.now().toString()); JTextField start = field(); JTextField end = field();
        JTextArea reason = new JTextArea(4, 25);
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        Object[] items = {"Offering ID (optional)", offering, "Medical date", medicalDate,
                "Start date", start, "End date", end, "Reason", new JScrollPane(reason)};
        for (int i = 0; i < items.length; i += 2) { panel.add(new JLabel((String) items[i])); panel.add((java.awt.Component) items[i + 1]); }
        if (JOptionPane.showConfirmDialog(this, panel, "Record medical", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Integer offeringId = offering.getText().isBlank() || offering.getText().equals("optional") ? null : integer(offering);
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a doctor's PDF certificate (maximum 10 MB)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        ApiResponse<?> response = medical.submitPdf(actor, new MedicalRow(0, 0, offeringId,
                LocalDate.parse(medicalDate.getText().trim()), LocalDate.parse(start.getText().trim()),
                LocalDate.parse(end.getText().trim()), reason.getText().trim(), "pending",
                ApprovalStatus.PENDING, false, null), chooser.getSelectedFile().toPath());
        show(response, "Medical request submitted as pending approval.");
    }

    private void decideMedical() {
        JTextField id = field();
        JComboBox<ApprovalStatus> decision = new JComboBox<>(new ApprovalStatus[]{ApprovalStatus.APPROVED, ApprovalStatus.REJECTED});
        JTextArea reason = new JTextArea(3, 22);
        if (!form("Decide medical", new String[]{"Medical ID", "Decision", "Reason (required for rejection)"},
                id, decision, new JScrollPane(reason))) return;
        show(medical.decide(actor, new MedicalController.DecideMedicalRequest(integer(id),
                (ApprovalStatus) decision.getSelectedItem(), reason.getText())), "Medical decision saved.");
    }

    private void viewPending() {
        showRecords(medical.pendingForReviewer(actor));
    }

    private void openProof() {
        JTextField id = field();
        if (!form("Open submitted PDF proof", new String[]{"Medical ID"}, id)) return;
        ApiResponse<java.nio.file.Path> response = medical.evidenceFor(actor, integer(id));
        if (!response.successful()) { error(response.error().message()); return; }
        try {
            PdfViewerDialog.show(this, "Medical proof", response.data());
        } catch (java.io.IOException invalidPdf) {
            error("The stored medical proof could not be opened.");
        }
    }

    private boolean form(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) { panel.add(new JLabel(labels[i])); panel.add(fields[i]); }
        return JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private JTextField field() { return new JTextField(18); }
    private int integer(JTextField field) { return Integer.parseInt(field.getText().trim()); }
    private void addButton(JPanel panel, String text, Runnable action) { javax.swing.JButton button = new javax.swing.JButton(text); button.addActionListener(event -> { try { action.run(); } catch (IllegalArgumentException | java.time.DateTimeException invalid) { error("Check IDs and dates (yyyy-MM-dd)."); } }); panel.add(button); }
    private void show(ApiResponse<?> response, String success) { if (response.successful()) JOptionPane.showMessageDialog(this, success, "Medical records", JOptionPane.INFORMATION_MESSAGE); else error(response.error().message()); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Medical records", JOptionPane.ERROR_MESSAGE); }
}
