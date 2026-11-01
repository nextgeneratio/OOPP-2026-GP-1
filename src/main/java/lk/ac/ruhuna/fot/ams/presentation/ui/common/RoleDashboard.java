package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.response.DashboardSummary;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Shared layout for the four role dashboards: summary cards plus shortcut buttons. Figures come from the controller. */
public abstract class RoleDashboard extends JPanel {
    private final Map<String, JLabel> valueLabels = new LinkedHashMap<>();
    private final JLabel stateLabel = new JLabel("Loading\u2026");

    protected RoleDashboard(String title, String subtitle, List<String> metricLabels, List<NavigationModule> shortcuts,
                            AuthenticatedSession session, DashboardController controller,
                            ApplicationErrorHandler errors, Consumer<NavigationModule> navigate) {
        super(new BorderLayout(0, UiTheme.SPACE_LG));
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(UiTheme.pageTitle());
        JLabel sub = new JLabel(subtitle);
        sub.setFont(UiTheme.body());
        sub.setForeground(UiTheme.MUTED);
        stateLabel.setFont(UiTheme.helper());
        stateLabel.setForeground(UiTheme.MUTED);
        header.add(heading);
        header.add(sub);
        header.add(stateLabel);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(sectionTitle("Overview"));
        JPanel cards = new JPanel(new GridLayout(0, 3, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        cards.setOpaque(false);
        metricLabels.forEach(label -> cards.add(card(label)));
        cards.setAlignmentX(LEFT_ALIGNMENT);
        body.add(cards);
        body.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_XL));
        body.add(sectionTitle("Shortcuts"));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        for (NavigationModule module : shortcuts) {
            AppButton button = new AppButton(module.label());
            button.addActionListener(event -> navigate.accept(module));
            actions.add(button);
        }
        body.add(actions);
        add(body, BorderLayout.CENTER);

        load(session, controller, errors);
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.sectionTitle());
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTheme.SPACE_SM, 0));
        return label;
    }

    private JPanel card(String label) {
        JPanel card = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        card.setBackground(UiTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD)));
        JLabel caption = new JLabel(label);
        caption.setFont(UiTheme.label());
        caption.setForeground(UiTheme.MUTED);
        JLabel value = new JLabel("\u2026");
        value.setFont(UiTheme.pageTitle());
        valueLabels.put(label, value);
        card.add(caption, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void load(AuthenticatedSession session, DashboardController controller, ApplicationErrorHandler errors) {
        new SwingWorker<DashboardSummary, Void>() {
            @Override protected DashboardSummary doInBackground() {
                return controller.load(session);
            }

            @Override protected void done() {
                try {
                    DashboardSummary summary = get();
                    valueLabels.forEach((label, value) -> value.setText(summary.value(label)));
                    stateLabel.setText(summary.metrics().isEmpty() ? "Summary figures are not available yet." : " ");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    valueLabels.values().forEach(value -> value.setText("—"));
                    stateLabel.setForeground(UiTheme.ERROR);
                    stateLabel.setText("\u26A0 " + errors.userMessage(failure.getCause()));
                }
            }
        }.execute();
    }
}
