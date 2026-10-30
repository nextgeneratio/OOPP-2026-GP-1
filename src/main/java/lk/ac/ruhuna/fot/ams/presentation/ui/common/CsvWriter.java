package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.List;

/** Turns a report into CSV text that opens correctly in spreadsheet programs (quoted fields, UTF-8 friendly). */
public final class CsvWriter {
    private CsvWriter() {
    }

    public static String toCsv(ReportResult report) {
        StringBuilder csv = new StringBuilder();
        appendLine(csv, report.columns());
        report.rows().forEach(row -> appendLine(csv, row));
        return csv.toString();
    }

    private static void appendLine(StringBuilder csv, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(cells.get(i)));
        }
        csv.append("\r\n");
    }

    private static String escape(String cell) {
        String value = cell == null ? "" : cell;
        // A leading =, +, - or @ would be run as a formula by spreadsheet programs, so mark it as text.
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuotes ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
