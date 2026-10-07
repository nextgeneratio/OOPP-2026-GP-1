package com.ams.ui.views.dashboard;

import javax.swing.*;
import java.awt.*;

public class TechOfficerDashboard extends JPanel {
    public TechOfficerDashboard() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Technical Officer Dashboard", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(label, BorderLayout.CENTER);
    }
}
