package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Cursor;
import javax.swing.JButton;

public final class AppButton extends JButton {
    public AppButton(String text) {
        super(text);
        setForeground(UiTheme.SURFACE);
        setBackground(UiTheme.PRIMARY);
        setFocusPainted(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
