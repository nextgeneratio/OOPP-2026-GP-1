package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Read-only table that shows whichever report is current. */
public final class ReportTableModel extends AbstractTableModel {
    private ReportResult result = new ReportResult("", List.of(), List.of());

    public void setResult(ReportResult newResult) {
        result = newResult;
        fireTableStructureChanged();
    }

    public void clear() {
        setResult(new ReportResult("", List.of(), List.of()));
    }

    @Override public int getRowCount() { return result.rows().size(); }
    @Override public int getColumnCount() { return result.columns().size(); }
    @Override public String getColumnName(int column) { return result.columns().get(column); }
    @Override public Object getValueAt(int row, int column) { return result.rows().get(row).get(column); }
    @Override public boolean isCellEditable(int row, int column) { return false; }
}
