package com.ams.ui.shared;

import com.ams.ui.components.CustomBadge;
import com.ams.ui.components.CustomButton;
import com.ams.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class TopBar extends JPanel {
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel userNameLabel;
    private CustomBadge roleBadge;
    private JPanel avatarPanel;
    private JLabel avatarInitials;
    private CustomButton logoutButton;

    public TopBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Left Branding Section
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        // Academic Logo Badge
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(36, 36));
        JLabel logoText = new JLabel("FT", SwingConstants.CENTER);
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoText.setForeground(Theme.PRIMARY);
        logoBadge.setLayout(new BorderLayout());
        logoBadge.add(logoText, BorderLayout.CENTER);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        titleLabel = new JLabel("FACULTY OF TECHNOLOGY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);

        subtitleLabel = new JLabel("Academic Management System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(200, 220, 240));

        titleBlock.add(titleLabel);
        titleBlock.add(subtitleLabel);

        leftPanel.add(logoBadge);
        leftPanel.add(titleBlock);

        // Right Section: User Info + Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        rightPanel.setOpaque(false);

        // User Avatar Circle
        avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        avatarPanel.setOpaque(false);
        avatarPanel.setPreferredSize(new Dimension(34, 34));
        avatarInitials = new JLabel("U", SwingConstants.CENTER);
        avatarInitials.setFont(new Font("Segoe UI", Font.BOLD, 12));
        avatarInitials.setForeground(Color.WHITE);
        avatarPanel.setLayout(new BorderLayout());
        avatarPanel.add(avatarInitials, BorderLayout.CENTER);

        userNameLabel = new JLabel("User Name");
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userNameLabel.setForeground(Color.WHITE);

        roleBadge = CustomBadge.warning("Role");

        logoutButton = new CustomButton("Logout", CustomButton.ButtonType.OUTLINE);
        logoutButton.setPreferredSize(new Dimension(90, 32));
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 12));

        rightPanel.add(avatarPanel);
        rightPanel.add(userNameLabel);
        rightPanel.add(roleBadge);
        rightPanel.add(logoutButton);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    public void setUserInfo(String name, String role) {
        userNameLabel.setText(name);
        
        // Compute initials
        String[] parts = name.split(" ");
        String initials = "";
        for (String p : parts) {
            if (!p.isEmpty() && !p.endsWith(".")) {
                initials += p.charAt(0);
                if (initials.length() >= 2) break;
            }
        }
        avatarInitials.setText(initials.isEmpty() ? "U" : initials.toUpperCase());

        // Role badge styling
        switch (role) {
            case "Admin":
                roleBadge.setText("ADMIN");
                break;
            case "Lecturer":
                roleBadge.setText("LECTURER");
                break;
            case "Tech Officer":
                roleBadge.setText("TECH OFFICER");
                break;
            case "Undergraduate":
                roleBadge.setText("STUDENT");
                break;
            default:
                roleBadge.setText(role.toUpperCase());
                break;
        }
        revalidate();
        repaint();
    }

    public void addLogoutListener(java.awt.event.ActionListener listener) {
        logoutButton.addActionListener(listener);
    }
}
