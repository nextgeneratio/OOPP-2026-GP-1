package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class TopBar extends JPanel {
    private JLabel titleLabel;
    private JLabel userLabel;
    private JButton logoutButton;

    public TopBar() {
        setLayout(new BorderLayout());
        setBackground(Theme.PRIMARY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM));

        titleLabel = new JLabel("Faculty of Technology AMS");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(Theme.FONT_TITLE);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACING_MEDIUM, 0));
        rightPanel.setOpaque(false);

        userLabel = new JLabel("Welcome, User (Role)");
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(Theme.FONT_REGULAR);

        logoutButton = new JButton("Logout");
        logoutButton.setFocusPainted(false);

        rightPanel.add(userLabel);
        rightPanel.add(logoutButton);

        add(titleLabel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }
    
    public void setUserInfo(String name, String role) {
        userLabel.setText(String.format("Welcome, %s (%s)", name, role));
    }
}
