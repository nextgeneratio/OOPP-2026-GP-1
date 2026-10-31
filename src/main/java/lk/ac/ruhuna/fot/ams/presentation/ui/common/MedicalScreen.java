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
        JComboBox<SelectionOption> offering = offeringSelector(false);
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
        JComboBox<SelectionOption> offering = offeringSelector(true);
        JTextField medicalDate = new JTextField(LocalDate.now().toString()); JTextField start = field(); JTextField end = field();
        JTextArea reason = new JTextArea(4, 25);
        if (!form("Record medical", new String[]{"Course offering (optional)", "Medical date", "Start date", "End date", "Reason"},
                offering, medicalDate, start, end, new JScrollPane(reason))) return;
        Integer offeringId = selectedId(offering)==0 ? null : selectedId(offering);
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
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) { panel.add(new JLabel(labels[i])); panel.add(fields[i]); }
        JScrollPane scroll = new JScrollPane(panel); scroll.setBorder(null);
        scroll.setPreferredSize(new java.awt.Dimension(620, Math.min(480, labels.length * 48 + 24)));
        return JOptionPane.showConfirmDialog(this, scroll, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }
    private JTextField field() { return new JTextField(18); }
    private JComboBox<SelectionOption> offeringSelector(boolean optional){
        var response=courses.offeringViewsForActor(actor);if(!response.successful()){error(response.error().message());return options(java.util.List.of());}
        java.util.ArrayList<SelectionOption> rows=new java.util.ArrayList<>();if(optional)rows.add(new SelectionOption(0,"All courses"));
        response.data().forEach(v->rows.add(new SelectionOption(v.offering().id(),v.course().code()+" — "+v.course().title()+" — "+v.semesterLabel())));return options(rows);
    }
    private JComboBox<SelectionOption> options(java.util.List<SelectionOption> values){JComboBox<SelectionOption> box=new JComboBox<>();values.forEach(box::addItem);if(values.isEmpty())box.addItem(new SelectionOption(0,"No matching records"));SelectionOption.enableSearch(box);return box;}
    private int selectedId(JComboBox<SelectionOption> box){Object value=box.getSelectedItem();if(value instanceof SelectionOption o)return o.id();throw new IllegalArgumentException("Choose a record.");}
    private String offeringLabel(Integer id){return courses.offeringViewsForActor(actor).data().stream().filter(v->v.offering().id()==id).map(v->v.course().code()+" — "+v.course().title()).findFirst().orElse("Course offering");}
    private void addButton(JPanel panel, String text, Runnable action) { javax.swing.JButton button = new javax.swing.JButton(text); button.addActionListener(event -> { try { action.run(); } catch (IllegalArgumentException | java.time.DateTimeException invalid) { error("Check the selected records and dates (yyyy-MM-dd)."); } }); panel.add(button); }
    private void show(ApiResponse<?> response, String success) { if (response.successful()) JOptionPane.showMessageDialog(this, success, "Medical records", JOptionPane.INFORMATION_MESSAGE); else error(response.error().message()); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Medical records", JOptionPane.ERROR_MESSAGE); }
}
