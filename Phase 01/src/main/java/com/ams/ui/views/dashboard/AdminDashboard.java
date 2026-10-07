package com.ams.ui.views.dashboard;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JPanel {
    public AdminDashboard() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Admin Dashboard", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(label, BorderLayout.CENTER);
    }
}
