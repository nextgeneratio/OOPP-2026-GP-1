package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class CardPanel extends JPanel {
    private int cornerRadius = Theme.RADIUS_LARGE;
    private Color cardBackground = Theme.SURFACE;
    private Color borderColor = Theme.CARD_BORDER;

    public CardPanel() {
        this(new BorderLayout());
    }

    public CardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM));
    }

    public void setCardBackground(Color color) {
        this.cardBackground = color;
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

        int width = getWidth();
        int height = getHeight();

        // Card background
        g2.setColor(cardBackground);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Subtle outline border
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);
    }
}
