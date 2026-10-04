package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import java.awt.*;

public class CustomComboBox<E> extends JComboBox<E> {
    public CustomComboBox(E[] items) {
        super(items);
        setFont(Theme.FONT_REGULAR);
        setForeground(Theme.TEXT_PRIMARY);
        setBackground(Color.WHITE);
        
        ((JLabel) getRenderer()).setBorder(BorderFactory.createEmptyBorder(Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL, Theme.SPACING_SMALL));
    }
}
