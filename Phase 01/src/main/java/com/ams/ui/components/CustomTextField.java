package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class CustomTextField extends JTextField {
    public CustomTextField(int columns) {
        super(columns);
        setFont(Theme.FONT_REGULAR);
        setForeground(Theme.TEXT_PRIMARY);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
                BorderFactory.createEmptyBorder(Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL)
        ));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.PRIMARY_COLOR, 1, true),
                        BorderFactory.createEmptyBorder(Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
                        BorderFactory.createEmptyBorder(Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL)
                ));
            }
        });
    }
}
