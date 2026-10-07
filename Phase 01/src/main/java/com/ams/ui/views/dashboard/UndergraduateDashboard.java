package com.ams.ui.views.dashboard;

import javax.swing.*;
import java.awt.*;

public class UndergraduateDashboard extends JPanel {
    public UndergraduateDashboard() {
        setLayout(new BorderLayout());
        JLabel label = new JLabel("Undergraduate Dashboard", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        add(label, BorderLayout.CENTER);
    }
}
