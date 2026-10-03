package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class StatusBar extends JPanel {
    private final JLabel message = new JLabel("Ready");

    public StatusBar(String signedInAs) {
        super(new BorderLayout());
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_SM - 2, UiTheme.SPACE_LG, UiTheme.SPACE_SM - 2, UiTheme.SPACE_LG)));
        message.setFont(UiTheme.helper());
        message.setForeground(UiTheme.MUTED);
        JLabel who = new JLabel(signedInAs);
        who.setFont(UiTheme.helper());
        who.setForeground(UiTheme.MUTED);
        add(message, BorderLayout.WEST);
        add(who, BorderLayout.EAST);
    }

    public void setMessage(String text) {
        message.setText(text);
    }
}
