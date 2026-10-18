package lk.ac.ruhuna.fot.ams.presentation.ui.admin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController.SearchUsersRequest;
import lk.ac.ruhuna.fot.ams.business.service.UserService.CreateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UpdateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UserView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ApiCall;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.Dialogs;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.FormField;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ObjectTableModel;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Admin user management. Collects input and renders results; every rule and permission lives in UserService. */
public final class UserManagementScreen extends JPanel {
    private static final String ALL_ROLES = "All roles";
    private static final String[] STATUSES = {"All statuses", "Active", "Inactive"};

    private final AuthenticatedSession session;
    private final UserController users;

    private final JTextField searchField = new JTextField(18);
    private final JComboBox<Object> roleFilter = new JComboBox<>();
    private final JComboBox<String> statusFilter = new JComboBox<>(STATUSES);
    private final JLabel tableState = new JLabel(" ");

    private final ObjectTableModel<UserView> model = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Username", UserView::username),
            ObjectTableModel.column("Full name", UserView::fullName),
            ObjectTableModel.column("Role", u -> NavigationModule.roleLabel(u.role())),
            ObjectTableModel.column("Email", UserView::email),
            ObjectTableModel.column("Status", u -> u.active() ? "ACTIVE" : "INACTIVE"),
            ObjectTableModel.column("Student no.", UserView::studentNumber)));
    private final JTable table = new JTable(model);

    private final JLabel formTitle = new JLabel("New user");
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JTextField emailField = new JTextField();
    private final JTextField fullNameField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField departmentField = new JTextField();
    private final JTextField batchField = new JTextField();
    private final JTextField studentNoField = new JTextField();
    private final JCheckBox activeBox = new JCheckBox("Account is active");
    private final FormField usernameRow = new FormField("Username", usernameField);
    private final FormField passwordRow = new FormField("Initial password", passwordField);
    private final FormField departmentRow = new FormField("Department ID (staff)", departmentField);
    private final FormField batchRow = new FormField("Batch ID (undergraduates)", batchField);
    private final JLabel formMessage = new JLabel(" ");
    private final AppButton saveButton = new AppButton("Create user");
    private final AppButton deactivateButton = new AppButton("Deactivate", true);

    private UserView editing;
    private Integer departmentId;
    private Integer batchId;

    public UserManagementScreen(AuthenticatedSession session, UserController users) {
        super(new BorderLayout(0, UiTheme.SPACE_MD));
        this.session = session;
        this.users = users;
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTablePanel(), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.EAST);
        startNew();
        search();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("User Management");
        title.setFont(UiTheme.pageTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);
        roleFilter.addItem(ALL_ROLES);
        for (Role role : Role.values()) {
            roleFilter.addItem(role);
        }
        roleFilter.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean selected, boolean focus) {
                Object shown = value instanceof Role role ? NavigationModule.roleLabel(role) : value;
                return super.getListCellRendererComponent(list, shown, index, selected, focus);
            }
        });
        searchField.getAccessibleContext().setAccessibleName("Search users");
        roleFilter.getAccessibleContext().setAccessibleName("Filter by role");
        statusFilter.getAccessibleContext().setAccessibleName("Filter by status");
        searchField.addActionListener(event -> search());
        AppButton searchButton = new AppButton("Search");
        searchButton.addActionListener(event -> search());
        AppButton resetButton = new AppButton("Reset", true);
        resetButton.addActionListener(event -> {
            searchField.setText("");
            roleFilter.setSelectedIndex(0);
            statusFilter.setSelectedIndex(0);
            search();
        });
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, UiTheme.SPACE_SM));
        filters.setOpaque(false);
        filters.setAlignmentX(LEFT_ALIGNMENT);
        filters.add(new JLabel("Search"));
        filters.add(searchField);
        filters.add(roleFilter);
        filters.add(statusFilter);
        filters.add(searchButton);
        filters.add(resetButton);
        header.add(title);
        header.add(filters);
        return header;
    }

    private JPanel buildTablePanel() {
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                edit(model.rowAt(table.convertRowIndexToModel(table.getSelectedRow())));
            }
        });
        tableState.setFont(UiTheme.helper());
        tableState.setForeground(UiTheme.MUTED);
        JPanel panel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        panel.setOpaque(false);
        panel.add(ObjectTableModel.styledScroll(table), BorderLayout.CENTER);
        panel.add(tableState, BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        formTitle.setFont(UiTheme.sectionTitle());
        formTitle.setAlignmentX(LEFT_ALIGNMENT);
        form.add(formTitle);
        form.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_MD));
        form.add(usernameRow);
        form.add(passwordRow);
        form.add(new FormField("Role", roleBox));
        form.add(new FormField("Full name", fullNameField));
        form.add(new FormField("Email", emailField));
        form.add(new FormField("Phone", phoneField));
        form.add(departmentRow);
        form.add(batchRow);
        form.add(new FormField("Student number (undergraduates)", studentNoField));
        activeBox.setAlignmentX(LEFT_ALIGNMENT);
        form.add(activeBox);
        formMessage.setFont(UiTheme.helper());
        formMessage.setForeground(UiTheme.ERROR);
        formMessage.setAlignmentX(LEFT_ALIGNMENT);
        formMessage.getAccessibleContext().setAccessibleName("Form message");
        form.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_SM));
        form.add(formMessage);
        form.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_SM));

        AppButton newButton = new AppButton("New user", true);
        newButton.addActionListener(event -> startNew());
        saveButton.addActionListener(event -> save());
        deactivateButton.addActionListener(event -> deactivate());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(saveButton);
        buttons.add(deactivateButton);
        buttons.add(newButton);
        form.add(buttons);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setPreferredSize(new Dimension(400, 0));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void startNew() {
        editing = null;
        table.clearSelection();
        formTitle.setText("New user");
        for (JTextField field : List.of(usernameField, emailField, fullNameField, phoneField,
                departmentField, batchField, studentNoField)) {
            field.setText("");
        }
        passwordField.setText("");
        roleBox.setSelectedItem(Role.UNDERGRADUATE);
        activeBox.setSelected(true);
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        activeBox.setEnabled(false);
        saveButton.setText("Create user");
        deactivateButton.setEnabled(false);
        clearMessages();
    }

    private void edit(UserView user) {
        editing = user;
        formTitle.setText("Edit " + user.username());
        usernameField.setText(user.username());
        usernameField.setEnabled(false);
        passwordField.setText("");
        passwordField.setEnabled(false);
        roleBox.setSelectedItem(user.role());
        emailField.setText(user.email());
        fullNameField.setText(user.fullName());
        phoneField.setText(user.phone() == null ? "" : user.phone());
        departmentField.setText(user.departmentId() == null ? "" : String.valueOf(user.departmentId()));
        batchField.setText(user.batchId() == null ? "" : String.valueOf(user.batchId()));
        studentNoField.setText(user.studentNumber() == null ? "" : user.studentNumber());
        activeBox.setSelected(user.active());
        activeBox.setEnabled(true);
        saveButton.setText("Save changes");
        deactivateButton.setEnabled(user.active());
        clearMessages();
    }

    private void clearMessages() {
        formMessage.setText(" ");
        departmentRow.clearError();
        batchRow.clearError();
    }

    private void search() {
        tableState.setForeground(UiTheme.MUTED);
        tableState.setText("Loading\u2026");
        String term = searchField.getText().isBlank() ? null : searchField.getText().trim();
        Object selectedRole = roleFilter.getSelectedItem();
        Role role = selectedRole instanceof Role r ? r : null;
        Boolean active = switch (statusFilter.getSelectedIndex()) {
            case 1 -> Boolean.TRUE;
            case 2 -> Boolean.FALSE;
            default -> null;
        };
        ApiCall.run(() -> users.search(session, new SearchUsersRequest(term, role, active)),
                result -> {
                    model.setRows(result);
                    tableState.setText(result.isEmpty() ? "No users match the current filters." : result.size() + " user(s)");
                },
                error -> {
                    model.setRows(List.of());
                    tableState.setForeground(UiTheme.ERROR);
                    tableState.setText("\u26A0 " + error.message());
                });
    }

    private boolean readNumbers() {
        departmentRow.clearError();
        batchRow.clearError();
        boolean valid = true;
        departmentId = null;
        batchId = null;
        if (!departmentField.getText().isBlank()) {
            try {
                departmentId = Integer.valueOf(departmentField.getText().trim());
            } catch (NumberFormatException invalid) {
                departmentRow.setError("Enter a whole number.");
                valid = false;
            }
        }
        if (!batchField.getText().isBlank()) {
            try {
                batchId = Integer.valueOf(batchField.getText().trim());
            } catch (NumberFormatException invalid) {
                batchRow.setError("Enter a whole number.");
                valid = false;
            }
        }
        return valid;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private void save() {
        formMessage.setText(" ");
        if (!readNumbers()) {
            return;
        }
        Role role = (Role) roleBox.getSelectedItem();
        setBusy(true);
        if (editing == null) {
            char[] password = passwordField.getPassword();
            CreateUser request = new CreateUser(usernameField.getText().trim(), password, role,
                    emailField.getText().trim(), fullNameField.getText().trim(), blankToNull(phoneField.getText()),
                    departmentId, batchId, blankToNull(studentNoField.getText()));
            ApiCall.run(() -> users.create(session, request), created -> finished("User created."), this::failed);
        } else {
            UpdateUser request = new UpdateUser(editing.id(), role, emailField.getText().trim(), activeBox.isSelected(),
                    fullNameField.getText().trim(), blankToNull(phoneField.getText()), departmentId, batchId,
                    blankToNull(studentNoField.getText()));
            ApiCall.run(() -> users.update(session, request), ignored -> finished("Changes saved."), this::failed);
        }
    }

    private void deactivate() {
        if (editing == null) {
            return;
        }
        UserView target = editing;
        if (!Dialogs.confirm(this, "Confirm deactivation", "Deactivate " + target.username()
                + "?\nThe account will no longer be able to sign in. Academic history is kept.")) {
            return;
        }
        setBusy(true);
        ApiCall.run(() -> users.deactivate(session, target.id()),
                ignored -> finished(target.username() + " was deactivated."), this::failed);
    }

    private void finished(String message) {
        setBusy(false);
        startNew();
        search();
        tableState.setText(message);
    }

    private void failed(ApiError error) {
        setBusy(false);
        if (Dialogs.isInline(error)) {
            formMessage.setText("\u26A0 " + error.message());
        } else {
            Dialogs.showError(this, error);
        }
    }

    private void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        deactivateButton.setEnabled(!busy && editing != null && editing.active());
    }
}
