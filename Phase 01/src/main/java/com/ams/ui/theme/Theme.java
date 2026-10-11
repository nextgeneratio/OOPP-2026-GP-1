package com.ams.ui.theme;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class Theme {
    // Primary Colors
    public static final Color PRIMARY = Color.decode("#123B5D");        // Deep Academic Navy
    public static final Color PRIMARY_HOVER = Color.decode("#1D5A87");  // Vibrant Blue
    public static final Color PRIMARY_LIGHT = Color.decode("#E8F1F8");  // Subtle Blue Tint
    
    // Secondary & Accent Colors
    public static final Color SECONDARY = Color.decode("#2F7D8C");      // Ocean Teal
    public static final Color SECONDARY_HOVER = Color.decode("#3A96A7");
    public static final Color SECONDARY_LIGHT = Color.decode("#E6F4F6");
    public static final Color ACCENT = Color.decode("#D9A441");         // Academic Gold
    public static final Color ACCENT_LIGHT = Color.decode("#FEF8EC");
    
    // Background & Surfaces
    public static final Color BACKGROUND = Color.decode("#F4F6F9");     // Soft Slate Gray
    public static final Color SURFACE = Color.decode("#FFFFFF");        // Crisp White
    public static final Color SURFACE_HOVER = Color.decode("#F8FAFC");  // Subtle Hover White
    public static final Color CARD_BORDER = Color.decode("#E2E8F0");    // Slate 200
    
    // Text Colors
    public static final Color TEXT_PRIMARY = Color.decode("#1E293B");   // Slate 800
    public static final Color TEXT_SECONDARY = Color.decode("#64748B"); // Slate 500
    public static final Color TEXT_MUTED = Color.decode("#94A3B8");     // Slate 400
    
    // Status Colors
    public static final Color SUCCESS = Color.decode("#16A34A");        // Emerald Green
    public static final Color SUCCESS_LIGHT = Color.decode("#DCFCE7");
    public static final Color WARNING = Color.decode("#D97706");        // Amber Orange
    public static final Color WARNING_LIGHT = Color.decode("#FEF3C7");
    public static final Color ERROR = Color.decode("#DC2626");          // Rose Red
    public static final Color ERROR_LIGHT = Color.decode("#FEE2E2");
    public static final Color INFO = Color.decode("#0284C7");           // Sky Blue
    public static final Color INFO_LIGHT = Color.decode("#E0F2FE");
    
    // Backward compatibility aliases
    public static final Color PRIMARY_COLOR = PRIMARY;
    public static final Color ACCENT_COLOR = ACCENT;
    public static final Color BACKGROUND_COLOR = BACKGROUND;

    // Spacing
    public static final int SPACING_XS = 4;
    public static final int SPACING_SMALL = 8;
    public static final int SPACING_MEDIUM = 16;
    public static final int SPACING_LARGE = 24;
    public static final int SPACING_XLARGE = 32;

    // Corner Radii
    public static final int RADIUS_SMALL = 6;
    public static final int RADIUS_MEDIUM = 10;
    public static final int RADIUS_LARGE = 14;

    // Fonts with smooth fallbacks
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL_BOLD = new Font("Segoe UI", Font.BOLD, 11);

    public static void setupUIManager() {
        try {
            // Enable antialiased text on modern Java runtimes
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.font", FONT_REGULAR);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", FONT_BOLD);
        UIManager.put("TextField.font", FONT_REGULAR);
        UIManager.put("PasswordField.font", FONT_REGULAR);
        UIManager.put("ComboBox.font", FONT_REGULAR);
        UIManager.put("Table.font", FONT_REGULAR);
        UIManager.put("TableHeader.font", FONT_BOLD);
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(SPACING_LARGE, SPACING_LARGE, SPACING_LARGE, SPACING_LARGE)
        );
    }
}
