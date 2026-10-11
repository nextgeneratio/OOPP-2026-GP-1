package com.ams.ui.components;

import com.ams.ui.theme.Theme;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

public class CustomTable extends JTable {

    public CustomTable(TableModel model) {
        super(model);
        initTable();
    }

    public CustomTable() {
        super();
        initTable();
    }

    private void initTable() {
        setFont(Theme.FONT_REGULAR);
        setRowHeight(38);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(Theme.CARD_BORDER);
        setSelectionBackground(Theme.PRIMARY_LIGHT);
        setSelectionForeground(Theme.PRIMARY);
        setFocusable(false);
        setIntercellSpacing(new Dimension(0, 1));

        // Table Header Styling
        JTableHeader header = getTableHeader();
        header.setFont(Theme.FONT_BOLD);
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(Theme.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.CARD_BORDER));

        // Cell padding renderer
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Theme.SURFACE : new Color(252, 253, 254));
                    c.setForeground(Theme.TEXT_PRIMARY);
                }
                return c;
            }
        });
    }

    public static JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.CARD_BORDER, 1, true));
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        return scrollPane;
    }
}
