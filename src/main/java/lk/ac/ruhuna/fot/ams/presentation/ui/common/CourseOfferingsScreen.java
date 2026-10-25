package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Course cards replace manual offering-ID lookup and expose student self-registration. */
public final class CourseOfferingsScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final CourseController courses;
    private final JPanel cards = new JPanel();

    public CourseOfferingsScreen(AuthenticatedSession actor, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.courses = courses;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        cards.setLayout(new javax.swing.BoxLayout(cards, javax.swing.BoxLayout.Y_AXIS));
        String message = actor.role() == Role.UNDERGRADUATE
                ? "Choose an open course to register; registered courses are listed first."
                : "Your assigned course offerings.";
        add(new JLabel("<html><h2>My course offerings</h2>" + message + "</html>"), BorderLayout.NORTH);
        add(new JScrollPane(cards), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh offerings");
        refresh.addActionListener(event -> refresh());
        add(refresh, BorderLayout.SOUTH);
        refresh();
    }

    private void refresh() {
        cards.removeAll();
        var enrolled = courses.offeringViewsForActor(actor);
        if (!enrolled.successful()) { error(enrolled.error().message()); return; }
        enrolled.data().forEach(view -> addCard(view, true));
        if (actor.role() == Role.UNDERGRADUATE) {
            var available = courses.availableOfferingsForCurrentStudent(actor);
            if (!available.successful()) { error(available.error().message()); return; }
            available.data().stream().filter(view -> !view.enrolled()).forEach(view -> addCard(view, false));
        }
        if (cards.getComponentCount() == 0) cards.add(new JLabel("No open course offerings are available."));
        cards.revalidate(); cards.repaint();
    }

    private void addCard(OfferingView view, boolean enrolled) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.add(new JLabel(String.format("<html><b>%s — %s</b><br/>%.1f credits · Semester %d · %s</html>",
                view.course().code(), view.course().title(), view.course().credits(),
                view.offering().semesterId(), enrolled ? "Registered" : "Open for registration")), BorderLayout.CENTER);
        if (!enrolled && actor.role() == Role.UNDERGRADUATE) {
            JButton register = new JButton("Register");
            register.addActionListener(event -> register(view));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.add(register); card.add(actions, BorderLayout.EAST);
        }
        cards.add(card); cards.add(javax.swing.Box.createVerticalStrut(8));
    }

    private void register(OfferingView view) {
        if (JOptionPane.showConfirmDialog(this, "Register for " + view.course().code() + "?", "Confirm registration",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        var response = courses.enrollCurrentStudent(actor, view.offering().id());
        if (response.successful()) { JOptionPane.showMessageDialog(this, "Registration completed."); refresh(); }
        else error(response.error().message());
    }
    private void error(String message) { JOptionPane.showMessageDialog(this, message, "Course offerings", JOptionPane.ERROR_MESSAGE); }
}
