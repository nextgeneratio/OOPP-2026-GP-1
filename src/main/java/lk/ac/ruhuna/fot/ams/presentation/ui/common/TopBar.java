package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class TopBar extends JPanel {
    public TopBar(AuthenticatedSession session, Runnable onProfile, Runnable onLogout) {
        super(new BorderLayout(UiTheme.SPACE_MD, 0));
        setBackground(UiTheme.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_SM + 4, UiTheme.SPACE_LG, UiTheme.SPACE_SM + 4, UiTheme.SPACE_LG));

        JLabel appName = new JLabel("Faculty of Technology AMS");
        appName.setFont(UiTheme.sectionTitle());
        appName.setForeground(UiTheme.SURFACE);
        add(appName, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACE_MD, 0));
        right.setOpaque(false);
        JLabel user = new JLabel(session.displayName() + "  |  " + NavigationModule.roleLabel(session.role()));
        user.setFont(UiTheme.body());
        user.setForeground(UiTheme.SURFACE);
        right.add(user);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem profile = new JMenuItem("My profile");
        profile.addActionListener(event -> onProfile.run());
        profile.setEnabled(NavigationModule.PROFILE.isAllowedFor(session.role()));
        menu.add(profile);
        JButton account = new AppButton("Account \u25BE", true);
        account.addActionListener(event -> menu.show(account, 0, account.getHeight()));
        account.getAccessibleContext().setAccessibleName("Account menu");
        right.add(account);

        JButton logout = new AppButton("Log out", true);
        logout.addActionListener(event -> onLogout.run());
        right.add(logout);
        add(right, BorderLayout.EAST);
    }
}
