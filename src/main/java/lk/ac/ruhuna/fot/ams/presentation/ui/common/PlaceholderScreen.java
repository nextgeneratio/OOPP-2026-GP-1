package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Shown for modules whose API/controller is not available yet. It never fakes data. */
public final class PlaceholderScreen extends JPanel {
    public PlaceholderScreen(NavigationModule module) {
        super(new GridBagLayout());
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        JLabel title = new JLabel(module.label());
        title.setFont(UiTheme.pageTitle());
        JLabel note = new JLabel("This screen is not connected yet. It will appear once its controller is available.");
        note.setFont(UiTheme.body());
        note.setForeground(UiTheme.MUTED);
        box.add(title);
        box.add(Box.createVerticalStrut(UiTheme.SPACE_SM));
        box.add(note);
        box.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(box);
    }
}
