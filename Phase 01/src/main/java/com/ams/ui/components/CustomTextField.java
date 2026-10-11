package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class CustomTextField extends JTextField {
    private boolean isFocused = false;
    private String placeholder = "";

    public CustomTextField(int columns) {
        this("", columns);
    }

    public CustomTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        initField();
    }

    private void initField() {
        setFont(Theme.FONT_REGULAR);
        setForeground(Theme.TEXT_PRIMARY);
        setBackground(Theme.SURFACE);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(isEnabled() ? Theme.SURFACE : Theme.BACKGROUND);
        g2.fillRoundRect(1, 1, width - 2, height - 2, Theme.RADIUS_MEDIUM, Theme.RADIUS_MEDIUM);

        // Border
        if (isFocused) {
            g2.setColor(Theme.PRIMARY_HOVER);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(Theme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(1, 1, width - 3, height - 3, Theme.RADIUS_MEDIUM, Theme.RADIUS_MEDIUM);

        super.paintComponent(g);

        // Placeholder
        if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
            g2.setColor(Theme.TEXT_MUTED);
            g2.setFont(Theme.FONT_REGULAR);
            FontMetrics fm = g2.getFontMetrics();
            int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(placeholder, 12, textY);
        }

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 38));
    }
}
