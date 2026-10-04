package com.ams.ui.theme;

import javax.swing.*;
import java.awt.*;

public class Theme {
    // Colors
    public static final Color PRIMARY_COLOR = Color.decode("#123B5D");
    public static final Color ACCENT_COLOR = Color.decode("#D9A441");
    public static final Color BACKGROUND_COLOR = Color.decode("#F5F7FA");
    public static final Color TEXT_PRIMARY = Color.decode("#333333");
    public static final Color TEXT_SECONDARY = Color.decode("#666666");
    
    // Spacing
    public static final int SPACING_SMALL = 8;
    public static final int SPACING_MEDIUM = 16;
    public static final int SPACING_LARGE = 24;
    public static final int SPACING_XLARGE = 32;

    // Fonts
    public static final Font FONT_REGULAR = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);

    public static void setupUIManager() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        UIManager.put("Panel.background", BACKGROUND_COLOR);
        UIManager.put("Label.font", FONT_REGULAR);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", FONT_BOLD);
    }
}
