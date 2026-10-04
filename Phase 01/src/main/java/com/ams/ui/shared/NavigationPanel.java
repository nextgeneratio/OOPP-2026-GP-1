package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class NavigationPanel extends JPanel {
    private JPanel menuPanel;

    public NavigationPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM, Theme.SPACING_MEDIUM));

        add(menuPanel, BorderLayout.NORTH);
    }

    public void setMenuOptions(List<String> options) {
        menuPanel.removeAll();
        for (String option : options) {
            JButton btn = new JButton(option);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            btn.setBackground(Color.WHITE);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            menuPanel.add(btn);
            menuPanel.add(Box.createRigidArea(new Dimension(0, Theme.SPACING_SMALL)));
        }
        menuPanel.revalidate();
        menuPanel.repaint();
    }
}
