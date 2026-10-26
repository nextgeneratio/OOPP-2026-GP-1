package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.NoticeRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Role-aware notice board, with creation restricted to authorised operational roles. */
public final class NoticesScreen extends JPanel {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final AuthenticatedSession actor;
    private final ContentController content;

    public NoticesScreen(AuthenticatedSession actor, ContentController content) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.content = content;
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Notices</h2>View current notices intended for your role or batch.</html>"),
                BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        addButton(actions, "Refresh notices", this::showNotices);
        if (actor.role() == Role.ADMIN || actor.role() == Role.TECHNICAL_OFFICER) {
            addButton(actions, "Publish notice", this::publishNotice);
        }
        add(actions, BorderLayout.CENTER);
        showNotices();
    }

    private void showNotices() {
        ApiResponse<java.util.List<NoticeRow>> response = content.visibleNotices(actor);
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        JTextArea output = new JTextArea(16, 70);
        output.setEditable(false);
        if (response.data().isEmpty()) {
            output.setText("There are no current notices for you.");
        } else {
            response.data().forEach(notice -> output.append(String.format(
                    "%s%nAudience: %s | Published: %s%s%n%n%s%n%n%s%n%n",
                    notice.title(), audienceLabel(notice), DATE_TIME.format(notice.publishDate()),
                    notice.expiryDate() == null ? "" : " | Expires: " + DATE_TIME.format(notice.expiryDate()),
                    notice.body(), "────────────────────────────────────")));
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(output), "Current notices",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void publishNotice() {
        JTextField title = new JTextField();
        JTextArea body = new JTextArea(5, 28);
        JComboBox<String> audience = new JComboBox<>(new String[]{"ALL", "ROLE", "BATCH"});
        JComboBox<Role> role = new JComboBox<>(Role.values());
        JTextField batchId = new JTextField();
        JTextField expiry = new JTextField("optional: yyyy-MM-dd HH:mm");
        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
        panel.add(new JLabel("Title")); panel.add(title);
        panel.add(new JLabel("Audience")); panel.add(audience);
        panel.add(new JLabel("Target role (ROLE only)")); panel.add(role);
        panel.add(new JLabel("Target batch ID (BATCH only)")); panel.add(batchId);
        panel.add(new JLabel("Expiry")); panel.add(expiry);
        panel.add(new JLabel("Message")); panel.add(new JScrollPane(body));
        if (JOptionPane.showConfirmDialog(this, panel, "Publish notice", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        String target = (String) audience.getSelectedItem();
        LocalDateTime expires = expiry.getText().startsWith("optional") || expiry.getText().isBlank()
                ? null : LocalDateTime.parse(expiry.getText().trim(), DATE_TIME);
        NoticeRow notice = new NoticeRow(0, title.getText().trim(), body.getText().trim(), target,
                "ROLE".equals(target) ? (Role) role.getSelectedItem() : null,
                "BATCH".equals(target) ? Integer.valueOf(batchId.getText().trim()) : null,
                LocalDateTime.now(), expires);
        ApiResponse<?> response = content.createNotice(actor, notice);
        if (response.successful()) {
            JOptionPane.showMessageDialog(this, "Notice published successfully.", "Notices",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            error(response.error().message());
        }
    }

    private String audienceLabel(NoticeRow notice) {
        return switch (notice.audience()) {
            case "ROLE" -> "Role: " + notice.targetRole();
            case "BATCH" -> "Batch ID: " + notice.targetBatchId();
            default -> "Everyone";
        };
    }

    private void addButton(JPanel panel, String text, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(text);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (IllegalArgumentException invalid) {
                error("Check the notice details and date format (yyyy-MM-dd HH:mm).");
            }
        });
        panel.add(button);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Notices", JOptionPane.ERROR_MESSAGE);
    }
}
