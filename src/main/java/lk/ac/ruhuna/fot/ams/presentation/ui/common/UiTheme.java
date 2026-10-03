package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.UIManager;

/** Single source of truth for colours, fonts and spacing. Screens must not define their own. */
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
    public static final Color SUCCESS = Color.decode("#2E7D32");
    public static final Color WARNING = Color.decode("#B7791F");
    public static final Color ERROR = Color.decode("#C62828");
    public static final Color INFO = Color.decode("#1565C0");

    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 16;
    public static final int SPACE_LG = 24;
    public static final int SPACE_XL = 32;

    private static final String FAMILY = Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()).contains("Inter")
            ? "Inter" : Font.SANS_SERIF;

    private UiTheme() {
    }

    public static Font font(int style, int size) {
        return new Font(FAMILY, style, size);
    }

    public static Font pageTitle() { return font(Font.BOLD, 26); }
    public static Font sectionTitle() { return font(Font.BOLD, 19); }
    public static Font body() { return font(Font.PLAIN, 14); }
    public static Font label() { return font(Font.PLAIN, 13); }
    public static Font table() { return font(Font.PLAIN, 13); }
    public static Font button() { return font(Font.BOLD, 14); }
    public static Font helper() { return font(Font.PLAIN, 12); }

    public static void apply() {
        FlatLightLaf.setup();
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", body());
        UIManager.put("Button.font", button());
        UIManager.put("TextField.font", body());
        UIManager.put("PasswordField.font", body());
        UIManager.put("ComboBox.font", body());
        UIManager.put("Table.font", table());
        UIManager.put("TableHeader.font", font(Font.BOLD, 13));
        UIManager.put("Component.focusColor", INFO);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Button.arc", 8);
    }
}
