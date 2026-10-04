package com.ams.ui.shared;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private JLabel statusLabel;

    public StatusBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        statusLabel = new JLabel("System Ready");
        statusLabel.setFont(Theme.FONT_REGULAR);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        add(statusLabel);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }
}
