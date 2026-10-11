package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomButton extends JButton {
    public enum ButtonType {
        PRIMARY, SECONDARY, OUTLINE, ACCENT, DANGER
    }

    private ButtonType type;
    private boolean isHovered = false;
    private int cornerRadius = Theme.RADIUS_MEDIUM;

    public CustomButton(String text) {
        this(text, ButtonType.PRIMARY);
    }

    public CustomButton(String text, boolean isPrimary) {
        this(text, isPrimary ? ButtonType.PRIMARY : ButtonType.OUTLINE);
    }

    public CustomButton(String text, ButtonType type) {
        super(text);
        this.type = type;
        initButton();
    }

    private void initButton() {
        setFont(Theme.FONT_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(Theme.SPACING_SMALL, Theme.SPACING_MEDIUM, Theme.SPACING_SMALL, Theme.SPACING_MEDIUM));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setButtonType(ButtonType type) {
        this.type = type;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        Color bgColor;
        Color textColor;
        Color borderColor = null;

        switch (type) {
            case SECONDARY:
                bgColor = isHovered ? Theme.SECONDARY_HOVER : Theme.SECONDARY;
                textColor = Color.WHITE;
                break;
            case ACCENT:
                bgColor = isHovered ? Theme.ACCENT.darker() : Theme.ACCENT;
                textColor = Color.WHITE;
                break;
            case DANGER:
                bgColor = isHovered ? Theme.ERROR.darker() : Theme.ERROR;
                textColor = Color.WHITE;
                break;
            case OUTLINE:
                bgColor = isHovered ? Theme.PRIMARY_LIGHT : Theme.SURFACE;
                textColor = isHovered ? Theme.PRIMARY : Theme.TEXT_PRIMARY;
                borderColor = isHovered ? Theme.PRIMARY : Theme.CARD_BORDER;
                break;
            case PRIMARY:
            default:
                bgColor = isHovered ? Theme.PRIMARY_HOVER : Theme.PRIMARY;
                textColor = Color.WHITE;
                break;
        }

        if (!isEnabled()) {
            bgColor = Theme.CARD_BORDER;
            textColor = Theme.TEXT_MUTED;
            borderColor = null;
        }

        // Fill background
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Draw border if outline
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        // Draw text
        g2.setColor(textColor);
        FontMetrics fm = g2.getFontMetrics(getFont());
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + Theme.SPACING_LARGE, 100), Math.max(d.height, 36));
    }
}
