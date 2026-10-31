package lk.ac.ruhuna.fot.ams.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.swing.JComboBox;
import org.junit.jupiter.api.Test;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.SelectionOption;

class SelectionOptionTest {
    @Test
    void searchMatchesBusinessLabelCaseInsensitivelyAndKeepsTheUnderlyingKey() {
        SelectionOption first = new SelectionOption(0, "Select a course…");
        SelectionOption course = new SelectionOption(42, "ICT2132 — Object Oriented Programming Practicum");
        JComboBox<SelectionOption> combo = new JComboBox<>(new SelectionOption[]{first, course});
        SelectionOption.enableSearch(combo);

        int match = combo.getKeySelectionManager().selectionForKey('i', combo.getModel());
        assertEquals(1, match);
        combo.setSelectedIndex(match);
        assertEquals(42, ((SelectionOption) combo.getSelectedItem()).id());
        assertEquals("ICT2132 — Object Oriented Programming Practicum",
                ((SelectionOption) combo.getSelectedItem()).label());
    }

    @Test
    void emptyModelsDoNotSelectAnInvalidIndex() {
        JComboBox<SelectionOption> combo = new JComboBox<>();
        SelectionOption.enableSearch(combo);
        assertEquals(-1, combo.getKeySelectionManager().selectionForKey('x', combo.getModel()));
        assertNull(combo.getSelectedItem());
    }
}
