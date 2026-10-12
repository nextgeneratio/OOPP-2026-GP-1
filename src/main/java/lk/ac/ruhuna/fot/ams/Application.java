package lk.ac.ruhuna.fot.ams;

import lk.ac.ruhuna.fot.ams.config.AppConfig;
import lk.ac.ruhuna.fot.ams.config.ApplicationComponents;
import lk.ac.ruhuna.fot.ams.presentation.ui.auth.LoginFrame;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load();
            ApplicationComponents components = ApplicationComponents.create(config);
            UiTheme.apply();
            SwingUtilities.invokeLater(() -> new LoginFrame(
                    components.authController(), components.errorHandler()).setVisible(true));
        } catch (Exception failure) {
            LOGGER.error("Application startup failed", failure);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "The application could not start. Check the local configuration.",
                    "Startup error",
                    JOptionPane.ERROR_MESSAGE));
        }
    }
}
