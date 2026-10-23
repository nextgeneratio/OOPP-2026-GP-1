package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.MainFrame;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiServices;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** The login window. All layout and behaviour live in LoginPanel; this class only opens the main window afterwards. */
public final class LoginFrame extends JFrame {
    private final AuthController authController;
    private final UiServices services;
    private final ApplicationErrorHandler errorHandler;

    public LoginFrame(AuthController authController, UiServices services, ApplicationErrorHandler errorHandler) {
        super("Academic Management System - Login");
        this.authController = authController;
        this.services = services;
        this.errorHandler = errorHandler;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        LoginPanel panel = new LoginPanel(authController::login, errorHandler, this::openShell);
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.defaultButton());
        setMinimumSize(new Dimension(900, 560));
        setSize(1100, 660);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) {
                panel.focusUsername();
            }
        });
    }

    private void openShell(AuthenticatedSession session) {
        dispose();
        new MainFrame(session, services, errorHandler,
                () -> new LoginFrame(authController, services, errorHandler).setVisible(true)).setVisible(true);
    }
}
