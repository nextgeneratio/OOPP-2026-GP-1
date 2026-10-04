package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomTable extends JTable {
    public CustomTable(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
        setFillsViewportHeight(true);
        setFont(Theme.FONT_REGULAR);
        setRowHeight(30);
        setSelectionBackground(Theme.PRIMARY_COLOR.brighter());
        setSelectionForeground(Color.WHITE);
        setGridColor(Color.LIGHT_GRAY);
        setShowVerticalLines(false);

        getTableHeader().setFont(Theme.FONT_BOLD);
        getTableHeader().setBackground(Theme.BACKGROUND_COLOR);
        getTableHeader().setForeground(Theme.TEXT_PRIMARY);
        getTableHeader().setReorderingAllowed(false);

        ((DefaultTableCellRenderer) getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
    }
}
