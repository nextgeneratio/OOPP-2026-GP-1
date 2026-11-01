package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

/** Read-only table model that displays values already prepared by the business layer. It computes nothing. */
public final class ObjectTableModel<T> extends AbstractTableModel {
    public record Column<T>(String name, Function<T, Object> value) {
    }

    private final List<Column<T>> columns;
    private final List<T> rows = new ArrayList<>();

    public ObjectTableModel(List<Column<T>> columns) {
        this.columns = List.copyOf(columns);
    }

    public static <T> Column<T> column(String name, Function<T, Object> value) {
        return new Column<>(name, value);
    }

    public void setRows(List<T> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public T rowAt(int index) {
        return rows.get(index);
    }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return columns.size(); }
    @Override public String getColumnName(int column) { return columns.get(column).name(); }

    @Override public Object getValueAt(int row, int column) {
        Object value = columns.get(column).value().apply(rows.get(row));
        return value == null ? "" : value;
    }

    @Override public boolean isCellEditable(int row, int column) { return false; }

    /** A consistently styled single-selection table wrapped in a scroll pane. */
    public static JScrollPane styledScroll(JTable table) {
        table.setFont(UiTheme.table());
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(UiTheme.font(java.awt.Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }
}
