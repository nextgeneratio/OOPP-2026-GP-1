package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

public final class Combos {
    private Combos() {
    }

    /** Shows each item as the given text, so combo boxes can hold controller records directly. */
    @SuppressWarnings("unchecked")
    public static <T> void describe(JComboBox<T> box, Function<T, String> text) {
        box.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value == null ? "" : text.apply((T) value),
                        index, selected, focus);
            }
        });
    }
}
