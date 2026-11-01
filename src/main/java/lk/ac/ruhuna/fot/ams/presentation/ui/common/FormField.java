package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Label, input and an inline message line directly beneath it. The message is text, never colour alone. */
public final class FormField extends JPanel {
    private final JLabel error = new JLabel(" ");
    private final JComponent input;

    public FormField(String labelText, JComponent input) {
        this.input = input;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(UiTheme.label());
        label.setLabelFor(input);
        label.setAlignmentX(LEFT_ALIGNMENT);
        input.setAlignmentX(LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(Integer.MAX_VALUE, input.getPreferredSize().height));
        input.getAccessibleContext().setAccessibleName(labelText);
        error.setFont(UiTheme.helper());
        error.setForeground(UiTheme.ERROR);
        error.setAlignmentX(LEFT_ALIGNMENT);
        add(label);
        add(Box.createVerticalStrut(UiTheme.SPACE_SM / 2));
        add(input);
        add(error);
        setBorder(BorderFactory.createEmptyBorder(0, 0, UiTheme.SPACE_SM, 0));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public JComponent input() {
        return input;
    }

    public void setError(String message) {
        error.setText(message == null || message.isBlank() ? " " : "\u26A0 " + message);
    }

    public void clearError() {
        setError(null);
    }
}
