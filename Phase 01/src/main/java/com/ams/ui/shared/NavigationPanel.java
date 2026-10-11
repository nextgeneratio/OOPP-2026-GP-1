package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class NavigationPanel extends JPanel {
    private JPanel menuPanel;
    private String selectedOption = "";

    public NavigationPanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(230, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.CARD_BORDER));

        // Content wrapper with scroll if needed
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        // Header label
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        headerPanel.setOpaque(false);
        JLabel menuTitle = new JLabel("PORTAL MODULES");
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuTitle.setForeground(Theme.TEXT_MUTED);
        headerPanel.add(menuTitle);

        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 16, 12));

        container.add(headerPanel, BorderLayout.NORTH);
        container.add(menuPanel, BorderLayout.CENTER);

        // Footer info card
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.CARD_BORDER),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel versionLabel = new JLabel("AMS Desktop Portal | v2.4");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setForeground(Theme.TEXT_MUTED);

        JLabel termLabel = new JLabel("Sem 1 | 2025/2026");
        termLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        termLabel.setForeground(Theme.TEXT_SECONDARY);

        footerPanel.add(termLabel, BorderLayout.NORTH);
        footerPanel.add(versionLabel, BorderLayout.SOUTH);

        add(container, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

    public void setMenuOptions(List<String> options) {
        menuPanel.removeAll();
        if (!options.isEmpty()) {
            selectedOption = options.get(0);
        }

        for (String option : options) {
            NavItemButton btn = new NavItemButton(option);
            menuPanel.add(btn);
            menuPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        }
        menuPanel.revalidate();
        menuPanel.repaint();
    }

    private class NavItemButton extends JPanel {
        private String text;
        private boolean isHovered = false;

        public NavItemButton(String text) {
            this.text = text;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(200, 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            JLabel label = new JLabel(text);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));

            if (text.equals(selectedOption)) {
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setForeground(Theme.PRIMARY);
            } else {
                label.setForeground(Theme.TEXT_PRIMARY);
            }

            add(label, BorderLayout.CENTER);

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

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedOption = text;
                    if (getParent() != null) {
                        getParent().repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isSelected = text.equals(selectedOption);

            if (isSelected) {
                g2.setColor(Theme.PRIMARY_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_MEDIUM, Theme.RADIUS_MEDIUM);
                // Active indicator bar
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(2, 6, 4, getHeight() - 12, 3, 3);
            } else if (isHovered) {
                g2.setColor(Theme.SURFACE_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.RADIUS_MEDIUM, Theme.RADIUS_MEDIUM);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
