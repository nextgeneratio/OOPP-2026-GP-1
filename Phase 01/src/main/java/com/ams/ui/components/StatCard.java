package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class StatCard extends CardPanel {

    public StatCard(String title, String value, String subtitle, Color accentColor, String iconText) {
        super(new BorderLayout(Theme.SPACING_MEDIUM, Theme.SPACING_SMALL));
        setPreferredSize(new Dimension(220, 110));

        // Top Header: Title and Icon Badge
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(Theme.TEXT_SECONDARY);

        // Icon circle badge
        JPanel iconBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        iconBadge.setOpaque(false);
        iconBadge.setPreferredSize(new Dimension(28, 28));
        JLabel iconLbl = new JLabel(iconText, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        iconLbl.setForeground(accentColor);
        iconBadge.setLayout(new BorderLayout());
        iconBadge.add(iconLbl, BorderLayout.CENTER);

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(iconBadge, BorderLayout.EAST);

        // Center: Big Value
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(Theme.TEXT_PRIMARY);

        // Bottom: Subtitle / trend indicator
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(accentColor);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.add(valueLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        contentPanel.add(subtitleLabel);

        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
}
