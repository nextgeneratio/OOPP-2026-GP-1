package com.ams;

import com.ams.ui.shared.MainFrame;
import com.ams.ui.shared.NavigationPanel;
import com.ams.ui.shared.TopBar;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            
            // Testing UI Phase 1 Setup
            // In a real app, these would be managed by controllers
            
            Component[] components = mainFrame.getContentPane().getComponents();
            for (Component c : components) {
                if (c instanceof TopBar) {
                    ((TopBar) c).setUserInfo("John Doe", "Admin");
                } else if (c instanceof NavigationPanel) {
                    ((NavigationPanel) c).setMenuOptions(Arrays.asList(
                        "Dashboard", "Users", "Courses", "Timetable", "Reports"
                    ));
                }
            }
            
            JPanel content = new JPanel();
            content.add(new JLabel("Phase 1 Complete - Welcome to AMS"));
            mainFrame.setContent(content);

            mainFrame.setVisible(true);
        });
    }
}
