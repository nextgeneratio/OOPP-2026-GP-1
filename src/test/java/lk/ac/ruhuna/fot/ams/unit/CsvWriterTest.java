package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.CsvWriter;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportResult;
import org.junit.jupiter.api.Test;

class CsvWriterTest {
    @Test
    void writesAHeaderAndOneLinePerRow() {
        var csv = CsvWriter.toCsv(new ReportResult("t", List.of("A", "B"), List.of(List.of("1", "2"), List.of("3", "4"))));

        assertThat(csv).isEqualTo("A,B\r\n1,2\r\n3,4\r\n");
    }

    @Test
    void quotesCommasQuotesAndLineBreaks() {
        var csv = CsvWriter.toCsv(new ReportResult("t", List.of("Reason"),
                List.of(List.of("rest, fever"), List.of("said \"ok\""), List.of("two\nlines"))));

        assertThat(csv).contains("\"rest, fever\"").contains("\"said \"\"ok\"\"\"").contains("\"two\nlines\"");
    }

    @Test
    void textThatLooksLikeAFormulaIsMarkedAsText() {
        var csv = CsvWriter.toCsv(new ReportResult("t", List.of("Name"), List.of(List.of("=HYPERLINK(\"x\")"), List.of("+1"))));

        assertThat(csv).contains("'=HYPERLINK").contains("'+1");
    }
}
