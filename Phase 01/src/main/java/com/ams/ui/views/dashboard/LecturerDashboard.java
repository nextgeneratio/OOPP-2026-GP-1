package com.ams.ui.views.dashboard;

import javax.swing.*;
import java.awt.*;

public class LecturerDashboard extends JPanel {
    public LecturerDashboard() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Lecturer Dashboard", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(label, BorderLayout.CENTER);
    }
}
