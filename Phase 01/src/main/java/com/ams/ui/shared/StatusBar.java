package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StatusBar extends JPanel {
    private JLabel statusLabel;
    private JLabel dateLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        // Left Section: Status Indicator Dot + Text
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JPanel statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.SUCCESS);
                g2.fillOval(1, 4, 8, 8);
                g2.dispose();
            }
        };
        statusDot.setOpaque(false);
        statusDot.setPreferredSize(new Dimension(10, 16));

        statusLabel = new JLabel("System Online | Database Connected (MySQL)");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        leftPanel.add(statusDot);
        leftPanel.add(statusLabel);

        // Right Section: Current Date / Academic Term
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        String dateStr = new SimpleDateFormat("EEEE, MMM dd, yyyy").format(new Date());
        dateLabel = new JLabel(dateStr + " | Faculty of Technology AMS");
        dateLabel.setFont(Theme.FONT_SMALL);
        dateLabel.setForeground(Theme.TEXT_MUTED);

        rightPanel.add(dateLabel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
