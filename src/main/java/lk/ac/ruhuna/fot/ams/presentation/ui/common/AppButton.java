package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

public final class AppButton extends JButton {
    private final Color normal;
    private final Color hover;

    public AppButton(String text) {
        this(text, false);
    }

    public AppButton(String text, boolean secondary) {
        super(text);
        normal = secondary ? UiTheme.SECONDARY : UiTheme.PRIMARY;
        hover = secondary ? UiTheme.PRIMARY : UiTheme.PRIMARY_HOVER;
        setFont(UiTheme.button());
        setForeground(UiTheme.SURFACE);
        setBackground(normal);
        setFocusPainted(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { setBackground(normal); }
        });
    }
}
