package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.List;

/** A report ready to show: column names and rows of display text. Every figure was prepared by the business layer. */
public record ReportResult(String title, List<String> columns, List<List<String>> rows) {
    public ReportResult {
        columns = List.copyOf(columns);
        rows = rows.stream().map(List::copyOf).toList();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
