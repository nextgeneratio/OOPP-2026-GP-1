package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class CustomBadge extends JLabel {
    private Color bgColor;
    private Color fgColor;

    public CustomBadge(String text, Color bgColor, Color fgColor) {
        super(text, SwingConstants.CENTER);
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        setFont(new Font("Segoe UI", Font.BOLD, 11));
        setForeground(fgColor);
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        setOpaque(false);
    }

    public static CustomBadge success(String text) {
        return new CustomBadge(text, Theme.SUCCESS_LIGHT, Theme.SUCCESS);
    }

    public static CustomBadge warning(String text) {
        return new CustomBadge(text, Theme.WARNING_LIGHT, Theme.WARNING);
    }

    public static CustomBadge error(String text) {
        return new CustomBadge(text, Theme.ERROR_LIGHT, Theme.ERROR);
    }

    public static CustomBadge info(String text) {
        return new CustomBadge(text, Theme.INFO_LIGHT, Theme.INFO);
    }

    public static CustomBadge primary(String text) {
        return new CustomBadge(text, Theme.PRIMARY_LIGHT, Theme.PRIMARY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
