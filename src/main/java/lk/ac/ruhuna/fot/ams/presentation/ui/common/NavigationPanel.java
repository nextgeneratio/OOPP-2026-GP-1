package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

/** Left menu listing only the modules permitted for the role. The selected item is bold and marked, not colour-only. */
public final class NavigationPanel extends JPanel {
    private final Map<NavigationModule, JButton> buttons = new EnumMap<>(NavigationModule.class);

    public NavigationPanel(Role role, Consumer<NavigationModule> onSelect) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_SM, UiTheme.SPACE_MD, UiTheme.SPACE_SM)));
        setPreferredSize(new Dimension(220, 0));
        for (NavigationModule module : NavigationModule.forRole(role)) {
            JButton button = new JButton(module.label());
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setFont(UiTheme.body());
            button.setForeground(UiTheme.TEXT);
            button.setBackground(UiTheme.SURFACE);
            button.setBorderPainted(false);
            button.setFocusPainted(true);
            button.addActionListener(event -> onSelect.accept(module));
            buttons.put(module, button);
            add(button);
            add(Box.createVerticalStrut(UiTheme.SPACE_SM / 2));
        }
    }

    public void select(NavigationModule selected) {
        buttons.forEach((module, button) -> {
            boolean active = module == selected;
            button.setText((active ? "\u25B8 " : "") + module.label());
            button.setFont(UiTheme.font(active ? Font.BOLD : Font.PLAIN, 14));
            button.setBackground(active ? UiTheme.BACKGROUND : UiTheme.SURFACE);
        });
    }
}
