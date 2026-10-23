package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.FormField;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Banner on the left, sign-in form on the right. It only collects the username and password and shows what the
 * controller answers; authentication and every message about a failed sign-in come from the business layer.
 */
public final class LoginPanel extends JPanel {
    private static final int FORM_WIDTH = 380;

    /** What the panel needs from the application: turn a login request into the controller's answer. */
    @FunctionalInterface
    public interface Authenticator {
        ApiResponse<AuthController.LoginResponse> authenticate(AuthController.LoginRequest request);
    }

    private final Authenticator authenticator;
    private final ApplicationErrorHandler errorHandler;
    private final Consumer<AuthenticatedSession> onSignedIn;

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final char defaultEchoChar = passwordField.getEchoChar();
    private final FormField usernameRow = new FormField("Username", usernameField);
    private final FormField passwordRow = new FormField("Password", passwordField);
    private final JCheckBox showPassword = new JCheckBox("Show password");
    private final JLabel capsLockLabel = new JLabel(" ");
    private final JLabel messageLabel = new JLabel(" ") {
        @Override public Dimension getPreferredSize() {
            int reserved = 2 * getFontMetrics(getFont()).getHeight() + UiTheme.SPACE_MD;
            return new Dimension(FORM_WIDTH, Math.max(super.getPreferredSize().height, reserved));
        }

        @Override public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    };
    private final AppButton loginButton = new AppButton("Login");

    public LoginPanel(Authenticator authenticator, ApplicationErrorHandler errorHandler,
                      Consumer<AuthenticatedSession> onSignedIn) {
        super(new GridBagLayout());
        this.authenticator = authenticator;
        this.errorHandler = errorHandler;
        this.onSignedIn = onSignedIn;

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.gridx = 0;
        constraints.weightx = 1.4;
        add(new BannerPanel(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.0;
        add(buildFormSide(), constraints);
        wireBehaviour();
    }

    public JButton defaultButton() {
        return loginButton;
    }

    public void focusUsername() {
        usernameField.requestFocusInWindow();
    }

    private JPanel buildFormSide() {
        JPanel content = new JPanel() {
            @Override public Dimension getPreferredSize() {
                return new Dimension(FORM_WIDTH, super.getPreferredSize().height);
            }
        };
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(UiTheme.pageTitle());
        welcome.setForeground(UiTheme.PRIMARY);
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        JLabel subtitle = new JLabel("Sign in to the Academic Management System");
        subtitle.setFont(UiTheme.body());
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        usernameField.putClientProperty("JTextField.placeholderText", "Your username, for example STU001");
        passwordField.putClientProperty("JTextField.placeholderText", "Your password");

        showPassword.setOpaque(false);
        showPassword.setFont(UiTheme.label());
        showPassword.setAlignmentX(LEFT_ALIGNMENT);
        capsLockLabel.setFont(UiTheme.helper());
        capsLockLabel.setForeground(UiTheme.WARNING);
        capsLockLabel.setAlignmentX(LEFT_ALIGNMENT);
        capsLockLabel.getAccessibleContext().setAccessibleName("Caps Lock warning");
        messageLabel.setFont(UiTheme.label());
        messageLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        messageLabel.setAlignmentX(LEFT_ALIGNMENT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_SM / 2, 0, UiTheme.SPACE_SM, 0));
        messageLabel.getAccessibleContext().setAccessibleName("Login message");

        loginButton.setAlignmentX(LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setPreferredSize(new Dimension(FORM_WIDTH, 42));

        JLabel help = new JLabel("Forgot your password? Contact your administrator.");
        help.setFont(UiTheme.helper());
        help.setForeground(UiTheme.MUTED);
        help.setAlignmentX(LEFT_ALIGNMENT);

        content.add(welcome);
        content.add(Box.createVerticalStrut(UiTheme.SPACE_SM / 2));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(UiTheme.SPACE_LG + UiTheme.SPACE_SM));
        content.add(usernameRow);
        content.add(passwordRow);
        content.add(showPassword);
        content.add(capsLockLabel);
        content.add(messageLabel);
        content.add(loginButton);
        content.add(Box.createVerticalStrut(UiTheme.SPACE_MD));
        content.add(help);

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setOpaque(false);
        centre.add(content);

        JLabel footer = new JLabel("\u00A9 University of Ruhuna \u00B7 Faculty of Technology", JLabel.CENTER);
        footer.setFont(UiTheme.helper());
        footer.setForeground(UiTheme.MUTED);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 0, UiTheme.SPACE_MD, 0));

        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(UiTheme.SURFACE);
        side.setPreferredSize(new Dimension(100, 660));
        side.setMinimumSize(new Dimension(FORM_WIDTH + 2 * UiTheme.SPACE_XL, 0));
        side.add(centre, BorderLayout.CENTER);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private void wireBehaviour() {
        usernameField.addActionListener(event -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(event -> login());
        loginButton.addActionListener(event -> login());
        showPassword.addActionListener(event ->
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEchoChar));

        usernameField.getDocument().addDocumentListener(new Typing(usernameRow));
        passwordField.getDocument().addDocumentListener(new Typing(passwordRow));

        passwordField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent event) { updateCapsLock(); }
            @Override public void keyReleased(KeyEvent event) { updateCapsLock(); }
        });
        passwordField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { updateCapsLock(); }
            @Override public void focusLost(FocusEvent event) { capsLockLabel.setText(" "); }
        });
    }

    /** Typing in a field clears that field's message and any general sign-in message. */
    private final class Typing implements DocumentListener {
        private final FormField row;

        private Typing(FormField row) {
            this.row = row;
        }

        private void typed() {
            row.clearError();
            messageLabel.setText(" ");
        }

        @Override public void insertUpdate(DocumentEvent event) { typed(); }
        @Override public void removeUpdate(DocumentEvent event) { typed(); }
        @Override public void changedUpdate(DocumentEvent event) { typed(); }
    }

    private void updateCapsLock() {
        boolean on;
        try {
            on = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException unavailable) {
            on = false;
        }
        capsLockLabel.setText(on ? "\u26A0 Caps Lock is on" : " ");
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        usernameRow.clearError();
        passwordRow.clearError();
        showMessage(null, false);
        boolean missingUsername = username.isEmpty();
        boolean missingPassword = password.length == 0;
        if (missingUsername) {
            usernameRow.setError("Enter your username.");
        }
        if (missingPassword) {
            passwordRow.setError("Enter your password.");
        }
        if (missingUsername || missingPassword) {
            Arrays.fill(password, '\0');
            (missingUsername ? usernameField : passwordField).requestFocusInWindow();
            return;
        }
        setBusy(true);
        passwordField.setText("");
        new SwingWorker<ApiResponse<AuthController.LoginResponse>, Void>() {
            @Override
            protected ApiResponse<AuthController.LoginResponse> doInBackground() {
                try {
                    return authenticator.authenticate(new AuthController.LoginRequest(username, password));
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    ApiResponse<AuthController.LoginResponse> response = get();
                    if (response.successful()) {
                        onSignedIn.accept(response.data().session());
                    } else {
                        failed(response.error().message());
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    failed("Sign-in was interrupted. Please try again.");
                } catch (ExecutionException failure) {
                    errorHandler.log(failure.getCause());
                    failed(errorHandler.userMessage(failure.getCause()));
                }
            }
        }.execute();
    }

    private void failed(String message) {
        showMessage(message, true);
        passwordField.requestFocusInWindow();
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        loginButton.setText(busy ? "Signing in\u2026" : "Login");
        if (busy) {
            showMessage("Signing in\u2026", false);
        }
    }

    private void showMessage(String text, boolean error) {
        messageLabel.setForeground(error ? UiTheme.ERROR : UiTheme.MUTED);
        if (text == null || text.isBlank()) {
            messageLabel.setText(" ");
            return;
        }
        messageLabel.setText(wrap((error ? "\u26A0 " : "") + text.trim()));
    }

    /** Breaks the text into lines that fit the form width; plain measuring is more reliable than HTML width styles. */
    private String wrap(String text) {
        java.awt.FontMetrics metrics = messageLabel.getFontMetrics(messageLabel.getFont());
        int maxWidth = FORM_WIDTH - 12;
        StringBuilder html = new StringBuilder("<html>");
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > maxWidth) {
                html.append(escape(line.toString())).append("<br>");
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        return html.append(escape(line.toString())).append("</html>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
