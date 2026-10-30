package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Component;
import javax.swing.JOptionPane;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;

/** Central place for confirmation and error dialogs so every screen reports problems the same way. */
public final class Dialogs {
    private Dialogs() {
    }

    public static boolean confirm(Component parent, String title, String message) {
        return JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public static void info(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Validation and business-rule messages belong next to the form; everything else is a dialog. */
    public static boolean isInline(ApiError error) {
        return "VALIDATION_ERROR".equals(error.code()) || "BUSINESS_RULE_VIOLATION".equals(error.code());
    }

    public static void showError(Component parent, ApiError error) {
        String title = switch (error.code()) {
            case "FORBIDDEN" -> "Permission required";
            case "NOT_FOUND" -> "Record unavailable";
            case "DATA_ACCESS_ERROR" -> "Database unavailable";
            default -> "Action not completed";
        };
        int type = "FORBIDDEN".equals(error.code()) ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
        JOptionPane.showMessageDialog(parent, error.message(), title, type);
    }
}
