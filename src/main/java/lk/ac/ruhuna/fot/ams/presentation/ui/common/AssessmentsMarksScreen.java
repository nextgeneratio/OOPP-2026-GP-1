package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.business.service.MarksService.MarkInput;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Lecturer assessment-plan and marks entry screen backed by the existing controllers. */
public final class AssessmentsMarksScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final AssessmentController assessments;
    private final MarksController marks;

    public AssessmentsMarksScreen(AuthenticatedSession actor, CourseController courses,
                                  AssessmentController assessments, MarksController marks) {
        super(new BorderLayout(12, 12));
        this.actor = actor; this.courses = courses;
        this.assessments = assessments; this.marks = marks;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Assessments & marks</h2>Publish a complete 100% assessment plan, then save marks for enrolled students.</html>"), BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        addButton(actions, "Publish assessment plan", this::publishPlan);
        addButton(actions, "View assessment plan", this::viewPlan);
        addButton(actions, "View enrolled students", this::viewRoster);
        addButton(actions, "Save a mark", this::saveMark);
        add(actions, BorderLayout.CENTER);
    }

    private JComboBox<OfferingView> offeringBox() {
        var response = courses.offeringViewsForActor(actor);
        if (!response.successful()) throw new IllegalArgumentException(response.error().message());
        if (response.data().isEmpty()) throw new IllegalArgumentException("No assigned course offerings are available.");
        JComboBox<OfferingView> box = new JComboBox<>(response.data().toArray(OfferingView[]::new));
        box.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof OfferingView view) setText(view.course().code() + " — " + view.course().title()
                        + " — " + view.semesterLabel());
                return this;
            }
        });
        SelectionOption.enableSearch(box);
        return box;
    }

    private void publishPlan() {
        JComboBox<OfferingView> offering = offeringBox();
        JTextArea plan = new JTextArea("Assignment|ASSIGNMENT|40|true\nFinal examination|FINAL|60|false", 6, 46);
        JPanel form = form("Course offering", offering, "One row: name|type|weight|CA true/false", new JScrollPane(plan));
        if (JOptionPane.showConfirmDialog(this, form, "Publish assessment plan", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        OfferingView selected = (OfferingView) offering.getSelectedItem();
        List<AssessmentRow> rows = new ArrayList<>();
        for (String line : plan.getText().lines().map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            String[] part = line.split("\\|", -1);
            if (part.length != 4) throw new IllegalArgumentException("Each assessment needs name|type|weight|CA.");
            if (!"true".equalsIgnoreCase(part[3].trim()) && !"false".equalsIgnoreCase(part[3].trim())) {
                throw new IllegalArgumentException("CA must be explicitly true or false.");
            }
            try {
                lk.ac.ruhuna.fot.ams.domain.enums.AssessmentType.valueOf(part[1].trim());
            } catch (IllegalArgumentException invalidType) {
                throw new IllegalArgumentException("Assessment type is invalid: " + part[1].trim());
            }
            BigDecimal weight;
            try {
                weight = new BigDecimal(part[2].trim());
            } catch (NumberFormatException invalidWeight) {
                throw new IllegalArgumentException("Assessment weight must be numeric.");
            }
            rows.add(new AssessmentRow(0, selected.offering().id(), part[0].trim(), part[1].trim(),
                    weight, Boolean.parseBoolean(part[3].trim())));
        }
        show(assessments.publishPlan(actor, new AssessmentController.PublishPlanRequest(selected.offering().id(), rows)),
                "Assessment plan published.");
    }

    private void viewRoster() {
        JComboBox<OfferingView> offering = offeringBox();
        if (JOptionPane.showConfirmDialog(this, form("Course offering", offering), "Enrolled students", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        OfferingView selected = (OfferingView) offering.getSelectedItem();
        var result = courses.rosterForOffering(actor, selected.offering().id());
        if (!result.successful()) { error(result.error().message()); return; }
        String text = result.data().stream()
                .map(row -> row.studentNumber() + " — " + row.fullName())
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, text.isBlank() ? "No active students are enrolled." : text,
                "Enrolled students", JOptionPane.INFORMATION_MESSAGE);
    }

    private void viewPlan() {
        OfferingView selected = chooseOffering("View assessment plan");
        if (selected == null) return;
        var result = assessments.assessmentsForOffering(actor, selected.offering().id());
        if (!result.successful()) { error(result.error().message()); return; }
        String text = result.data().stream().map(assessment -> assessment.name() + " | "
                + assessment.type() + " | " + assessment.weight() + "% | "
                + (assessment.caComponent() ? "CA" : "Final"))
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, text.isBlank() ? "No assessment plan has been published."
                : text, "Assessment plan", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveMark() {
        OfferingView selected = chooseOffering("Save a mark");
        if (selected == null) return;
        var assessmentResult = assessments.assessmentsForOffering(actor, selected.offering().id());
        var rosterResult = courses.rosterForOffering(actor, selected.offering().id());
        if (!assessmentResult.successful()) { error(assessmentResult.error().message()); return; }
        if (!rosterResult.successful()) { error(rosterResult.error().message()); return; }
        if (assessmentResult.data().isEmpty()) { error("Publish an assessment plan before entering marks."); return; }
        if (rosterResult.data().isEmpty()) { error("No active student enrolments are available."); return; }
        JComboBox<AssessmentRow> assessmentBox = new JComboBox<>(assessmentResult.data().toArray(AssessmentRow[]::new));
        assessmentBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof AssessmentRow assessment) {
                    setText(assessment.name() + " (" + assessment.type() + ", weight " + assessment.weight() + "%)");
                }
                return this;
            }
        });
        SelectionOption.enableSearch(assessmentBox);
        JComboBox<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow> studentBox =
                new JComboBox<>(rosterResult.data().toArray(
                        lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow[]::new));
        studentBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow rosterEntry) {
                    setText(rosterEntry.studentNumber() + " — " + rosterEntry.fullName());
                }
                return this;
            }
        });
        SelectionOption.enableSearch(studentBox);
        JTextField score = new JTextField();
        if (JOptionPane.showConfirmDialog(this, form("Assessment", assessmentBox, "Student", studentBox,
                "Score", score), "Save a mark", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        AssessmentRow assessment = (AssessmentRow) assessmentBox.getSelectedItem();
        var enrollment = (lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow) studentBox.getSelectedItem();
        if (assessment == null || enrollment == null) { error("Choose an assessment and student."); return; }
        BigDecimal parsedScore;
        try {
            parsedScore = new BigDecimal(score.getText().trim());
        } catch (NumberFormatException invalidScore) {
            error("Score must be a valid number.");
            return;
        }
        if (parsedScore.signum() < 0 || parsedScore.compareTo(BigDecimal.valueOf(100)) > 0) {
            error("Score must be between 0 and 100.");
            return;
        }
        show(marks.saveMarks(actor, new MarksController.SaveMarksRequest(selected.offering().id(),
                List.of(new MarkInput(assessment.id(), enrollment.enrollmentId(), parsedScore)))), "Mark saved.");
    }

    private OfferingView chooseOffering(String title) {
        JComboBox<OfferingView> offering = offeringBox();
        if (JOptionPane.showConfirmDialog(this, form("Course offering", offering), title,
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return null;
        return (OfferingView) offering.getSelectedItem();
    }

    private JPanel form(Object... pairs) { JPanel p = new JPanel(new GridLayout(pairs.length / 2, 2, 8, 8)); for (int i=0;i<pairs.length;i+=2) { p.add(new JLabel((String)pairs[i])); p.add((java.awt.Component)pairs[i+1]); } return p; }
    private void addButton(JPanel panel, String title, Runnable action) { javax.swing.JButton b = new javax.swing.JButton(title); b.addActionListener(e -> { try { action.run(); } catch (RuntimeException x) { error(x.getMessage()); } }); panel.add(b); }
    private void show(lk.ac.ruhuna.fot.ams.api.controller.ApiResponse<?> response, String success) { if (response.successful()) JOptionPane.showMessageDialog(this, success, "Assessments & marks", JOptionPane.INFORMATION_MESSAGE); else error(response.error().message()); }
    private void error(String message) { JOptionPane.showMessageDialog(this, message == null ? "Check the entered values." : message, "Assessments & marks", JOptionPane.ERROR_MESSAGE); }
}
