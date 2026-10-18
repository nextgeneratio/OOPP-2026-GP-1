package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class CourseOfferingsScreen extends javax.swing.JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final JTextArea results = new JTextArea();

    public CourseOfferingsScreen(AuthenticatedSession actor, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        results.setEditable(false);
        add(new javax.swing.JLabel("<html><h2>Course offerings</h2>Use these offering IDs in "
                + "materials, quizzes, attendance, and timetable actions.</html>"), BorderLayout.NORTH);
        add(new JScrollPane(results), BorderLayout.CENTER);
        javax.swing.JButton refresh = new javax.swing.JButton("Refresh offerings");
        refresh.addActionListener(event -> refresh());
        add(refresh, BorderLayout.SOUTH);
    }

    private void refresh() {
        var response = courses.offeringsForActor(actor);
        if (!response.successful()) {
            JOptionPane.showMessageDialog(this, response.error().message(),
                    "Course offerings", JOptionPane.ERROR_MESSAGE);
            return;
        }
        results.setText("");
        response.data().forEach(row -> results.append(String.format(
                "Offering %d | course %d | batch %d | semester %d | coordinator %s | %s%n",
                row.id(), row.courseId(), row.batchId(), row.semesterId(),
                row.coordinatorId(), row.status())));
        if (response.data().isEmpty()) {
            results.setText("No course offerings are currently assigned or enrolled.");
        }
    }
}
