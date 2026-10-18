package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ObjectTableModel;
import org.junit.jupiter.api.Test;

class ObjectTableModelTest {
    private final ObjectTableModel<String> model = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Text", value -> value),
            ObjectTableModel.column("Length", String::length),
            ObjectTableModel.column("Missing", value -> null)));

    @Test
    void displaysRowsAndNeverEditsCells() {
        model.setRows(List.of("alpha", "be"));
        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getColumnName(1)).isEqualTo("Length");
        assertThat(model.getValueAt(0, 1)).isEqualTo(5);
        assertThat(model.isCellEditable(0, 0)).isFalse();
    }

    @Test
    void showsNullValuesAsEmptyText() {
        model.setRows(List.of("x"));
        assertThat(model.getValueAt(0, 2)).isEqualTo("");
    }

    @Test
    void replacingRowsDropsOldOnes() {
        model.setRows(List.of("a", "b", "c"));
        model.setRows(List.of("z"));
        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.rowAt(0)).isEqualTo("z");
    }
}
