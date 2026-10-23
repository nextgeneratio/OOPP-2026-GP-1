package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController.LoginResponse;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

/** Exercises the real login panel with a fake authenticator, so no database or controller is involved. */
class LoginPanelTest {
    private static final AuthenticatedSession SESSION = new AuthenticatedSession(1L, "ADM001", Role.ADMIN, "Admin");

    private final AtomicInteger signInAttempts = new AtomicInteger();
    private final AtomicReference<AuthenticatedSession> signedIn = new AtomicReference<>();

    private LoginPanel panel(LoginPanel.Authenticator authenticator) {
        return new LoginPanel(request -> {
            signInAttempts.incrementAndGet();
            return authenticator.authenticate(request);
        }, null, signedIn::set);
    }

    @Test
    void emptyFieldsShowMessagesNextToTheFieldsAndDoNotSignIn() {
        LoginPanel panel = panel(request -> ApiResponse.success(new LoginResponse(SESSION)));

        button(panel, "Login").doClick();

        assertThat(labelTexts(panel)).anyMatch(text -> text.contains("Enter your username."))
                .anyMatch(text -> text.contains("Enter your password."));
        assertThat(signInAttempts.get()).isZero();
        assertThat(signedIn.get()).isNull();
    }

    @Test
    void typingInAFieldClearsItsMessage() {
        LoginPanel panel = panel(request -> ApiResponse.success(new LoginResponse(SESSION)));
        button(panel, "Login").doClick();

        first(panel, JTextField.class, field -> !(field instanceof JPasswordField)).setText("ADM001");

        assertThat(labelTexts(panel)).noneMatch(text -> text.contains("Enter your username."));
        assertThat(labelTexts(panel)).anyMatch(text -> text.contains("Enter your password."));
    }

    @Test
    void showPasswordRevealsAndMasksTheTypedPassword() {
        LoginPanel panel = panel(request -> ApiResponse.success(new LoginResponse(SESSION)));
        JPasswordField password = first(panel, JPasswordField.class, field -> true);
        JCheckBox show = first(panel, JCheckBox.class, box -> true);
        char masked = password.getEchoChar();
        assertThat(masked).isNotEqualTo((char) 0);

        show.doClick();
        assertThat(password.getEchoChar()).isEqualTo((char) 0);

        show.doClick();
        assertThat(password.getEchoChar()).isEqualTo(masked);
    }

    @Test
    void aValidLoginHandsTheSessionOnAndClearsThePasswordField() throws Exception {
        LoginPanel panel = panel(request -> ApiResponse.success(new LoginResponse(SESSION)));
        fill(panel, "ADM001", "Ams@Dev2026");

        button(panel, "Login").doClick();

        waitUntil(() -> signedIn.get() != null);
        assertThat(signedIn.get().username()).isEqualTo("ADM001");
        assertThat(first(panel, JPasswordField.class, field -> true).getPassword()).isEmpty();
    }

    @Test
    void aFailedLoginShowsOnlyTheSafeMessageAndStaysOnTheLoginScreen() throws Exception {
        LoginPanel panel = panel(request -> ApiResponse.failure(
                new ApiError("AUTHENTICATION_FAILED", "Username or password is incorrect.")));
        fill(panel, "ADM001", "wrong");

        button(panel, "Login").doClick();

        waitUntil(() -> labelTexts(panel).stream().anyMatch(text -> text.contains("Username or password is incorrect.")));
        assertThat(signedIn.get()).isNull();
        assertThat(labelTexts(panel)).noneMatch(text -> text.contains("Exception") || text.contains("SELECT"));
        assertThat(button(panel, "Login").isEnabled()).isTrue();
    }

    private static void fill(LoginPanel panel, String username, String password) {
        first(panel, JTextField.class, field -> !(field instanceof JPasswordField)).setText(username);
        first(panel, JPasswordField.class, field -> true).setText(password);
    }

    private static JButton button(Container root, String text) {
        return first(root, JButton.class, button -> text.equals(button.getText()));
    }

    private static List<String> labelTexts(Container root) {
        List<String> texts = new ArrayList<>();
        collect(root, JLabel.class).forEach(label -> texts.add(label.getText()));
        return texts;
    }

    private static <T extends Component> T first(Container root, Class<T> type, java.util.function.Predicate<T> filter) {
        return collect(root, type).stream().filter(filter).findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> collect(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(collect(container, type));
            }
        }
        return found;
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        for (int attempt = 0; attempt < 50 && !condition.getAsBoolean(); attempt++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).as("the screen reached the expected state").isTrue();
    }
}
