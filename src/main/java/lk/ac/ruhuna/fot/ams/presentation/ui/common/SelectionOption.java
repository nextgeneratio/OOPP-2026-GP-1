package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import javax.swing.JList;
import javax.swing.JLabel;
import java.awt.Component;

/** A business-facing selector label which retains its real database key internally. */
public record SelectionOption(int id, String label) {
    @Override public String toString() { return label; }

    /** Enables case-insensitive type-to-search while keeping the selected object as the model value. */
    public static void enableSearch(JComboBox<?> combo) {
        combo.setToolTipText("Type to search options; use the arrow keys to choose.");
        combo.setKeySelectionManager(new JComboBox.KeySelectionManager() {
            private String prefix = "";
            private long lastKeyAt;

            @Override public int selectionForKey(char key, ComboBoxModel<?> model) {
                long now = System.currentTimeMillis();
                prefix = now - lastKeyAt > 900 ? "" : prefix;
                lastKeyAt = now;
                prefix += Character.toLowerCase(key);
                int start = Math.max(0, combo.getSelectedIndex());
                for (int offset = 1; offset <= model.getSize(); offset++) {
                    int index = (start + offset) % model.getSize();
                    Object item = model.getElementAt(index);
                    if (item != null && visibleText(combo, item, index).toLowerCase(java.util.Locale.ROOT).contains(prefix)) {
                        return index;
                    }
                }
                return -1;
            }
        });
    }

    private static String visibleText(JComboBox<?> combo, Object item, int index) {
        @SuppressWarnings("unchecked")
        javax.swing.ListCellRenderer<Object> renderer = (javax.swing.ListCellRenderer<Object>) combo.getRenderer();
        Component rendered = renderer.getListCellRendererComponent(new JList<>(), item, index, false, false);
        return rendered instanceof JLabel label ? label.getText() : item.toString();
    }
}
