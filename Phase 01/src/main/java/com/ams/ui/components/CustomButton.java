package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomButton extends JButton {
    private boolean isPrimary;

    public CustomButton(String text, boolean isPrimary) {
        super(text);
        this.isPrimary = isPrimary;
        setFont(Theme.FONT_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(true);
        updateColors();
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(isPrimary ? Theme.PRIMARY_COLOR.brighter() : Color.LIGHT_GRAY);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateColors();
            }
        });
    }

    private void updateColors() {
        if (isPrimary) {
            setBackground(Theme.PRIMARY_COLOR);
            setForeground(Color.WHITE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Theme.TEXT_PRIMARY);
        }
    }
}
