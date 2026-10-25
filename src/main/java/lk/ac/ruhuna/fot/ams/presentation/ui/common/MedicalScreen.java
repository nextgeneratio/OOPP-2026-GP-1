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
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Medical records screen. Technical officers create and decide records; other roles may view scoped records. */
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
        addButton(actions, "View student medicals", this::viewRecords);
        if (actor.role() == Role.TECHNICAL_OFFICER) {
            addButton(actions, "Record medical", this::recordMedical);
            addButton(actions, "Approve or reject medical", this::decideMedical);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void viewRecords() {
        JTextField student = field();
        JTextField offering = new JTextField("optional");
        if (!form("View medical records", new String[]{"Student ID", "Offering ID (optional)"}, student, offering)) return;
        Integer offeringId = offering.getText().isBlank() || offering.getText().equals("optional")
                ? null : integer(offering);
        ApiResponse<java.util.List<MedicalRow>> response = medical.findForStudent(actor,
                new MedicalController.MedicalSearchRequest(integer(student), offeringId));
        if (!response.successful()) { error(response.error().message()); return; }
        JTextArea output = new JTextArea(14, 70);
        output.setEditable(false);
        response.data().forEach(row -> output.append(String.format(
                "Medical %d | offering %s | %s to %s | %s%n%s%nEvidence: %s%n%n",
                row.id(), row.offeringId() == null ? "all courses" : row.offeringId(), row.startDate(), row.endDate(),
                row.status(), row.reason(), row.evidenceReference() == null ? "not recorded" : row.evidenceReference())));
        if (response.data().isEmpty()) output.setText("No medical records match this student.");
        JOptionPane.showMessageDialog(this, new JScrollPane(output), "Medical records", JOptionPane.INFORMATION_MESSAGE);
    }

    private void recordMedical() {
        JTextField student = field(); JTextField offering = new JTextField("optional");
        JTextField medicalDate = new JTextField(LocalDate.now().toString()); JTextField start = field(); JTextField end = field();
        JTextArea reason = new JTextArea(4, 25); JTextField evidence = new JTextField("optional evidence reference");
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        Object[] items = {"Student ID", student, "Offering ID (optional)", offering, "Medical date", medicalDate,
                "Start date", start, "End date", end, "Reason", new JScrollPane(reason), "Evidence reference", evidence};
        for (int i = 0; i < items.length; i += 2) { panel.add(new JLabel((String) items[i])); panel.add((java.awt.Component) items[i + 1]); }
        if (JOptionPane.showConfirmDialog(this, panel, "Record medical", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        Integer offeringId = offering.getText().isBlank() || offering.getText().equals("optional") ? null : integer(offering);
        String evidenceValue = evidence.getText().startsWith("optional") || evidence.getText().isBlank() ? null : evidence.getText().trim();
        ApiResponse<?> response = medical.submit(actor, new MedicalRow(0, integer(student), offeringId,
                LocalDate.parse(medicalDate.getText().trim()), LocalDate.parse(start.getText().trim()),
                LocalDate.parse(end.getText().trim()), reason.getText().trim(), evidenceValue,
                ApprovalStatus.PENDING, false));
        show(response, "Medical record saved as pending approval.");
    }

    private void decideMedical() {
        JTextField id = field();
        JComboBox<ApprovalStatus> decision = new JComboBox<>(new ApprovalStatus[]{ApprovalStatus.APPROVED, ApprovalStatus.REJECTED});
        if (!form("Decide medical", new String[]{"Medical ID", "Decision"}, id, decision)) return;
        show(medical.decide(actor, new MedicalController.DecideMedicalRequest(integer(id),
                (ApprovalStatus) decision.getSelectedItem())), "Medical decision saved.");
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
