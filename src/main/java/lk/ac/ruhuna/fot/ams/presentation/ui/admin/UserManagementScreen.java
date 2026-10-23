package lk.ac.ruhuna.fot.ams.presentation.ui.admin;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.business.service.UserService.CreateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UpdateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UserView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class UserManagementScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final UserController users;
    private final ApplicationErrorHandler errors;
    private final Role fixedRole;
    private final JTextField search = new JTextField(20);
    private final JComboBox<String> roleFilter = new JComboBox<>();
    private final JComboBox<String> activeFilter = new JComboBox<>(new String[]{"All accounts", "Active", "Inactive"});
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Username", "Role", "Full name", "Email", "Status", "ID"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private List<UserView> rows = List.of();

    public UserManagementScreen(
            AuthenticatedSession actor, UserController users, ApplicationErrorHandler errors, Role fixedRole) {
        super(new BorderLayout(UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        this.actor = actor;
        this.users = users;
        this.errors = errors;
        this.fixedRole = fixedRole;
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel(fixedRole == Role.UNDERGRADUATE ? "Student Management" : "User Management");
        title.setFont(UiTheme.pageTitle());
        header.add(title, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        filters.setOpaque(false);
        filters.add(new JLabel("Search"));
        filters.add(search);
        if (fixedRole == null) {
            roleFilter.addItem("All roles");
            Arrays.stream(Role.values()).forEach(role -> roleFilter.addItem(role.name()));
            filters.add(roleFilter);
        }
        filters.add(activeFilter);
        AppButton refresh = new AppButton("Search / Refresh");
        refresh.addActionListener(event -> reload());
        filters.add(refresh);
        header.add(filters, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.removeColumn(table.getColumnModel().getColumn(5));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        actions.setOpaque(false);
        if (actor.role() == Role.ADMIN && fixedRole == null) {
            AppButton add = new AppButton("Add account");
            add.addActionListener(event -> createUser());
            actions.add(add);
        }
        AppButton edit = new AppButton("Edit selected");
        edit.addActionListener(event -> editUser());
        actions.add(edit);
        if (actor.role() == Role.ADMIN) {
            AppButton deactivate = new AppButton("Deactivate selected");
            deactivate.addActionListener(event -> deactivateUser());
            actions.add(deactivate);
        }
        footer.add(actions, BorderLayout.WEST);
        status.setFont(UiTheme.helper());
        status.setForeground(UiTheme.MUTED);
        footer.add(status, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        reload();
    }

    private void reload() {
        Role role = fixedRole;
        if (role == null && roleFilter.getSelectedIndex() > 0) {
            role = Role.valueOf((String) roleFilter.getSelectedItem());
        }
        Boolean active = switch (activeFilter.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
        Role selectedRole = role;
        String term = search.getText().trim();
        setStatus("Loading accounts...");
        new SwingWorker<ApiResponse<List<UserView>>, Void>() {
            @Override
            protected ApiResponse<List<UserView>> doInBackground() {
                return users.search(actor, new UserController.SearchUsersRequest(
                        term, selectedRole, active));
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<List<UserView>> response = get();
                    if (!response.successful()) {
                        showError(response.error().message());
                        return;
                    }
                    rows = response.data();
                    model.setRowCount(0);
                    for (UserView row : rows) {
                        model.addRow(new Object[]{row.username(), row.role(), row.fullName(),
                                row.email(), row.active() ? "Active" : "Inactive", row.id()});
                    }
                    setStatus(rows.size() + (rows.size() == 1 ? " account" : " accounts"));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    setStatus("Loading accounts was interrupted.");
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    showError(errors.userMessage(failure.getCause()));
                }
            }
        }.execute();
    }

    private void createUser() {
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<Role> role = new JComboBox<>(Role.values());
        role.setSelectedItem(Role.UNDERGRADUATE);
        JTextField email = new JTextField();
        JTextField fullName = new JTextField();
        JTextField phone = new JTextField();
        JTextField departmentId = new JTextField("1");
        JTextField batchId = new JTextField("1");
        JTextField studentNumber = new JTextField();
        JPanel form = form(new Object[][]{
                {"Username", username}, {"Initial password", password}, {"Role", role},
                {"Email", email}, {"Full name", fullName}, {"Phone", phone},
                {"Department ID (staff)", departmentId}, {"Batch ID (student)", batchId},
                {"Student number (student)", studentNumber}
        });
        int choice = JOptionPane.showConfirmDialog(this, form, "Create account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] rawPassword = password.getPassword();
        password.setText("");
        if (choice != JOptionPane.OK_OPTION) {
            Arrays.fill(rawPassword, '\0');
            return;
        }
        Role selectedRole = (Role) role.getSelectedItem();
        Integer department;
        Integer batch;
        try {
            department = selectedRole == Role.LECTURER || selectedRole == Role.TECHNICAL_OFFICER
                    ? parseOptionalId(departmentId.getText()) : null;
            batch = selectedRole == Role.UNDERGRADUATE ? parseOptionalId(batchId.getText()) : null;
        } catch (NumberFormatException failure) {
            showError("Department and batch IDs must be whole numbers.");
            Arrays.fill(rawPassword, '\0');
            return;
        }
        String studentNo = selectedRole == Role.UNDERGRADUATE ? studentNumber.getText().trim() : null;
        CreateUser request = new CreateUser(username.getText(), rawPassword, selectedRole, email.getText(),
                fullName.getText(), phone.getText(), department, batch, studentNo);
        runMutation(() -> {
            try {
                return users.create(actor, request);
            } finally {
                Arrays.fill(rawPassword, '\0');
            }
        }, "Account created.");
    }

    private void editUser() {
        UserView selected = selectedUser();
        if (selected == null) {
            return;
        }
        JTextField email = new JTextField(selected.email());
        JTextField fullName = new JTextField(selected.fullName());
        JTextField phone = new JTextField(selected.phone() == null ? "" : selected.phone());
        JTextField departmentId = new JTextField(text(selected.departmentId()));
        JTextField batchId = new JTextField(text(selected.batchId()));
        JTextField studentNumber = new JTextField(
                selected.studentNumber() == null ? "" : selected.studentNumber());
        JPanel form = form(new Object[][]{
                {"Username", new JLabel(selected.username())}, {"Role", new JLabel(selected.role().name())},
                {"Email", email}, {"Full name", fullName}, {"Phone", phone},
                {"Department ID (staff)", departmentId}, {"Batch ID (student)", batchId},
                {"Student number (student)", studentNumber}
        });
        if (JOptionPane.showConfirmDialog(this, form, "Edit account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        Integer department;
        Integer batch;
        try {
            department = parseForRole(selected.role(), departmentId.getText(), batchId.getText(), true);
            batch = parseForRole(selected.role(), departmentId.getText(), batchId.getText(), false);
        } catch (NumberFormatException failure) {
            showError("Department and batch IDs must be whole numbers.");
            return;
        }
        UpdateUser request = new UpdateUser(selected.id(), selected.role(), email.getText(),
                selected.active(), fullName.getText(), phone.getText(), department, batch,
                selected.role() == Role.UNDERGRADUATE ? studentNumber.getText().trim() : null);
        runMutation(() -> users.update(actor, request), "Account updated.");
    }

    private void deactivateUser() {
        UserView selected = selectedUser();
        if (selected == null) {
            return;
        }
        if (selected.id() == actor.userId()) {
            showError("You cannot deactivate the account currently signed in.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this,
                "Deactivate " + selected.username() + "? This prevents future sign-ins.",
                "Confirm deactivation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        runMutation(() -> users.deactivate(actor, selected.id()), "Account deactivated.");
    }

    private UserView selectedUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select an account first.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        long id = ((Number) model.getValueAt(modelRow, 5)).longValue();
        return rows.stream().filter(user -> user.id() == id).findFirst().orElse(null);
    }

    private void runMutation(Supplier<ApiResponse<?>> request, String successMessage) {
        setStatus("Saving...");
        new SwingWorker<ApiResponse<?>, Void>() {
            @Override
            protected ApiResponse<?> doInBackground() {
                return request.get();
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<?> response = get();
                    if (!response.successful()) {
                        showError(response.error().message());
                        return;
                    }
                    setStatus(successMessage);
                    reload();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    setStatus("Saving was interrupted.");
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    showError(errors.userMessage(failure.getCause()));
                }
            }
        }.execute();
    }

    private JPanel form(Object[][] fields) {
        JPanel form = new JPanel(new GridLayout(fields.length, 2, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        for (Object[] field : fields) {
            form.add(new JLabel((String) field[0]));
            form.add((java.awt.Component) field[1]);
        }
        return form;
    }

    private Integer parseOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.valueOf(value.trim());
    }

    private Integer parseForRole(Role role, String departmentId, String batchId, boolean department) {
        if (department && (role == Role.LECTURER || role == Role.TECHNICAL_OFFICER)) {
            return parseOptionalId(departmentId);
        }
        if (!department && role == Role.UNDERGRADUATE) {
            return parseOptionalId(batchId);
        }
        return null;
    }

    private String text(Integer value) {
        return value == null ? "" : value.toString();
    }

    private void setStatus(String message) {
        status.setText(message);
        status.setForeground(UiTheme.MUTED);
    }

    private void showError(String message) {
        status.setText(message);
        status.setForeground(UiTheme.ERROR);
        JOptionPane.showMessageDialog(this, message, "Account management", JOptionPane.ERROR_MESSAGE);
    }
}
