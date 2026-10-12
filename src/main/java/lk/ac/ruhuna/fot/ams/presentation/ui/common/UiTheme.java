package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

public final class UiTheme {
    public static final Color PRIMARY = Color.decode("#123B5D");
    public static final Color PRIMARY_HOVER = Color.decode("#1D5A87");
    public static final Color SECONDARY = Color.decode("#2F7D8C");
    public static final Color ACCENT = Color.decode("#D9A441");
    public static final Color BACKGROUND = Color.decode("#F5F7FA");
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = Color.decode("#1F2937");
    public static final Color MUTED = Color.decode("#667085");
    public static final Color BORDER = Color.decode("#D0D5DD");
    public static final Color ERROR = Color.decode("#C62828");
    public static final Color SUCCESS = Color.decode("#2E7D32");

    private UiTheme() {
    }

    public static void apply() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.BOLD, 14));
        UIManager.put("TextField.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }
}
