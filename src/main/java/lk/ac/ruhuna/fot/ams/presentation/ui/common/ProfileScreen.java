package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.business.service.ProfileService.ProfileView;
import lk.ac.ruhuna.fot.ams.business.service.ProfilePictureStore;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ProfileScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final ProfileController profiles;
    private final ApplicationErrorHandler errors;
    private final boolean canChangePassword;
    private final JLabel username = new JLabel("Loading...");
    private final JLabel role = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JLabel accountDetail = new JLabel(" ");
    private final JTextField fullName = new JTextField(28);
    private final JTextField email = new JTextField(28);
    private final JTextField phone = new JTextField(28);
    private final JPasswordField currentPassword = new JPasswordField(28);
    private final JPasswordField newPassword = new JPasswordField(28);
    private final JPasswordField passwordConfirmation = new JPasswordField(28);
    private final AppButton save = new AppButton("Save profile");
    private final AppButton changePassword = new AppButton("Change password");
    private final AppButton choosePicture = new AppButton("Choose picture", true);
    private final JLabel picture = new JLabel("No picture");
    private final ProfilePictureStore pictureStore = new ProfilePictureStore(
            Path.of("src", "main", "resources", "uploads"));
    private ProfileView loadedProfile;

    public ProfileScreen(
            AuthenticatedSession actor, ProfileController profiles, ApplicationErrorHandler errors) {
        super(new BorderLayout(0, UiTheme.SPACE_LG));
        this.actor = actor;
        this.profiles = profiles;
        this.errors = errors;
        this.canChangePassword = actor.role() != Role.UNDERGRADUATE;
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));

        JLabel heading = new JLabel("My Profile");
        heading.setFont(UiTheme.pageTitle());
        JPanel header = new JPanel(new BorderLayout(UiTheme.SPACE_LG, 0));
        header.setOpaque(false);
        header.add(heading, BorderLayout.WEST);
        JPanel picturePanel = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        picturePanel.setOpaque(false);
        picture.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        picture.setPreferredSize(new java.awt.Dimension(96, 96));
        picture.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        choosePicture.addActionListener(event -> choosePicture());
        picturePanel.add(picture, BorderLayout.CENTER);
        picturePanel.add(choosePicture, BorderLayout.SOUTH);
        header.add(picturePanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        configureValue(username);
        configureValue(role);
        configureValue(accountDetail);
        configureInput(email);
        configureInput(phone);
        username.setText(actor.username());
        role.setText(actor.role().name());
        accountDetail.setText(value(actor.displayName()));
        email.setText("Loading...");
        phone.setText("Loading...");

        JPanel form = new JPanel(new GridLayout(0, 2, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        form.setOpaque(false);
        form.add(new JLabel("Profile details"));
        form.add(new JLabel(" "));
        form.add(new JLabel("Username"));
        form.add(username);
        form.add(new JLabel("Role"));
        form.add(role);
        form.add(new JLabel("Full name"));
        form.add(fullName);
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Phone"));
        form.add(phone);
        form.add(new JLabel("Role details"));
        form.add(accountDetail);
        form.add(new JLabel(" "));
        form.add(status);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG,
                        UiTheme.SPACE_LG, UiTheme.SPACE_LG)));

        JPanel passwordForm = new JPanel(new GridLayout(0, 2, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        passwordForm.setOpaque(false);
        passwordForm.add(new JLabel("Change password"));
        passwordForm.add(new JLabel(" "));
        passwordForm.add(new JLabel("Current password"));
        passwordForm.add(currentPassword);
        passwordForm.add(new JLabel("New password (at least 8 characters)"));
        passwordForm.add(newPassword);
        passwordForm.add(new JLabel("Confirm new password"));
        passwordForm.add(passwordConfirmation);
        passwordForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG,
                        UiTheme.SPACE_LG, UiTheme.SPACE_LG)));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new javax.swing.BoxLayout(body, javax.swing.BoxLayout.Y_AXIS));
        body.add(form);
        save.addActionListener(event -> saveProfile());
        changePassword.addActionListener(event -> submitPasswordChange());
        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, UiTheme.SPACE_SM, 0));
        actions.setOpaque(false);
        actions.add(save);
        if (canChangePassword) {
            body.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_MD));
            body.add(passwordForm);
            actions.add(changePassword);
        }
        body.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_MD));
        body.add(actions);
        JScrollPane profileScroll = new JScrollPane(body);
        profileScroll.setBorder(null);
        profileScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        profileScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        profileScroll.getVerticalScrollBar().setUnitIncrement(18);
        profileScroll.getViewport().setOpaque(false);
        add(profileScroll, BorderLayout.CENTER);
        fullName.setEditable(false);
        save.setEnabled(false);
        changePassword.setEnabled(false);
        reload();
    }

    private void reload() {
        save.setEnabled(false);
        status.setText("Loading profile...");
        new SwingWorker<ApiResponse<ProfileView>, Void>() {
            @Override
            protected ApiResponse<ProfileView> doInBackground() {
                return profiles.ownProfile(actor);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<ProfileView> response = get();
                    if (!response.successful()) {
                        showError(response.error().message());
                        return;
                    }
                    loadedProfile = response.data();
                    username.setText(loadedProfile.username());
                    role.setText(loadedProfile.role().name());
                    fullName.setText(value(loadedProfile.fullName()));
                    email.setText(loadedProfile.email());
                    phone.setText(value(loadedProfile.phone()));
                    accountDetail.setText(switch (loadedProfile.role()) {
                        case ADMIN -> "Administrator account";
                        case LECTURER, TECHNICAL_OFFICER ->
                                "Department: " + value(loadedProfile.departmentName());
                        case UNDERGRADUATE -> "Department: " + value(loadedProfile.departmentName())
                                + "  |  Batch: " + value(loadedProfile.batchCode())
                                + "  |  Level: " + value(loadedProfile.academicLevel())
                                + "  |  Semester: " + value(loadedProfile.semesterNumber())
                                + "  |  Student number: " + value(loadedProfile.studentNumber());
                    });
                    setPicture(loadedProfile.profilePicture());
                    boolean editableContact = loadedProfile.role() != Role.ADMIN;
                    fullName.setEditable(editableContact);
                    phone.setEditable(editableContact);
                    email.setEditable(loadedProfile.role() == Role.ADMIN);
                    save.setEnabled(loadedProfile.active());
                    changePassword.setEnabled(canChangePassword && loadedProfile.active());
                    status.setForeground(UiTheme.MUTED);
                    status.setText(loadedProfile.active() ? " " : "This account is inactive.");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showError("Loading your profile was interrupted.");
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    showError(errors.userMessage(failure.getCause()));
                }

            }
        }.execute();
    }

    private void choosePicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a PNG or JPEG profile picture");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        try {
            byte[] contents = Files.readAllBytes(selected);
            choosePicture.setEnabled(false);
            status.setText("Uploading profile picture...");
            new SwingWorker<ApiResponse<Void>, Void>() {
                @Override protected ApiResponse<Void> doInBackground() {
                    return profiles.updateProfilePicture(actor,
                            new ProfileController.ProfilePictureRequest(
                                    selected.getFileName().toString(), contents));
                }

                @Override protected void done() {
                    try {
                        ApiResponse<Void> response = get();
                        if (!response.successful()) {
                            showError(response.error().message());
                        } else {
                            status.setText("Profile picture saved.");
                            reload();
                        }
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        showError("Uploading the profile picture was interrupted.");
                    } catch (ExecutionException failure) {
                        errors.log(failure.getCause());
                        showError(errors.userMessage(failure.getCause()));
                    } finally {
                        choosePicture.setEnabled(loadedProfile != null && loadedProfile.active());
                    }
                }
            }.execute();
        } catch (IOException failure) {
            showError("Unable to read the selected image.");
        }
    }

    private void setPicture(String storedReference) {
        ImageIcon icon = pictureStore.icon(storedReference, 96, 96);
        picture.setIcon(icon);
        picture.setText(icon == null ? "No picture" : "");
    }

    private void saveProfile() {
        if (loadedProfile == null) {
            return;
        }
        ProfileView profile = loadedProfile;
        String updatedEmail = email.getText();
        String updatedName = fullName.getText();
        String updatedPhone = phone.getText();
        save.setEnabled(false);
        status.setText("Saving profile...");
        new SwingWorker<ApiResponse<Void>, Void>() {
            @Override
            protected ApiResponse<Void> doInBackground() {
                if (profile.role() == Role.UNDERGRADUATE) {
                    return profiles.updateUndergraduate(actor,
                            new ProfileController.UndergraduateProfileRequest(
                                    updatedName, updatedPhone));
                }
                return profiles.updateStaff(actor,
                        new ProfileController.StaffProfileRequest(
                                updatedEmail, updatedName, updatedPhone));
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (!response.successful()) {
                        showError(response.error().message());
                        save.setEnabled(true);
                        return;
                    }
                    status.setForeground(UiTheme.MUTED);
                    status.setText("Profile saved.");
                    reload();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showError("Saving your profile was interrupted.");
                    save.setEnabled(true);
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    showError(errors.userMessage(failure.getCause()));
                    save.setEnabled(true);
                }
            }
        }.execute();
    }

    private void submitPasswordChange() {
        char[] current = currentPassword.getPassword();
        char[] replacement = newPassword.getPassword();
        char[] confirmation = passwordConfirmation.getPassword();
        currentPassword.setText("");
        newPassword.setText("");
        passwordConfirmation.setText("");
        changePassword.setEnabled(false);
        status.setForeground(UiTheme.MUTED);
        status.setText("Changing password...");
        new SwingWorker<ApiResponse<Void>, Void>() {
            @Override
            protected ApiResponse<Void> doInBackground() {
                try {
                    return profiles.changePassword(actor,
                            new ProfileController.ChangePasswordRequest(current, replacement, confirmation));
                } finally {
                    java.util.Arrays.fill(current, '\0');
                    java.util.Arrays.fill(replacement, '\0');
                    java.util.Arrays.fill(confirmation, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    ApiResponse<Void> response = get();
                    if (!response.successful()) {
                        showError(response.error().message());
                    } else {
                        status.setForeground(UiTheme.MUTED);
                        status.setText("Password changed. Use the new password the next time you sign in.");
                    }
                    changePassword.setEnabled(canChangePassword && loadedProfile != null && loadedProfile.active());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    showError("Changing your password was interrupted.");
                    changePassword.setEnabled(canChangePassword && loadedProfile != null && loadedProfile.active());
                } catch (ExecutionException failure) {
                    errors.log(failure.getCause());
                    showError(errors.userMessage(failure.getCause()));
                    changePassword.setEnabled(canChangePassword && loadedProfile != null && loadedProfile.active());
                }
            }
        }.execute();
    }

    private String value(Object value) {
        return value == null || value.toString().isBlank() ? "Not set" : value.toString();
    }

    private void configureValue(JLabel label) {
        label.setForeground(java.awt.Color.BLACK);
        label.setFont(UiTheme.body());
        label.setOpaque(true);
        label.setBackground(java.awt.Color.WHITE);
        label.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        label.setMinimumSize(new java.awt.Dimension(220, 32));
        label.setPreferredSize(new java.awt.Dimension(320, 32));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void configureInput(JTextField field) {
        field.setForeground(java.awt.Color.BLACK);
        field.setBackground(java.awt.Color.WHITE);
        field.setCaretColor(java.awt.Color.BLACK);
        field.setMinimumSize(new java.awt.Dimension(220, 32));
        field.setPreferredSize(new java.awt.Dimension(320, 32));
    }

    private void showError(String message) {
        status.setForeground(UiTheme.ERROR);
        status.setText(message);
    }
}
