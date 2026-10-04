package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private TopBar topBar;
    private NavigationPanel navigationPanel;
    private StatusBar statusBar;
    private JPanel contentPanel;

    public MainFrame() {
        setTitle("Academic Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Theme.setupUIManager();

        topBar = new TopBar();
        navigationPanel = new NavigationPanel();
        statusBar = new StatusBar();
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Theme.BACKGROUND_COLOR);

        add(topBar, BorderLayout.NORTH);
        add(navigationPanel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void setContent(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
