package com.ams.ui.views;

import com.ams.ui.components.CardPanel;
import com.ams.ui.components.CustomButton;
import com.ams.ui.components.CustomPasswordField;
import com.ams.ui.components.CustomTextField;
import com.ams.ui.theme.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

public class LoginView extends JPanel {

    private CustomTextField usernameField;
    private CustomPasswordField passwordField;
    private CustomButton loginButton;
    private JPanel errorBanner;
    private JLabel errorLabel;
    private JLabel loadingLabel;
    private BufferedImage backgroundImage;

    public LoginView() {
        setLayout(new GridBagLayout());
        loadBackgroundImage();
        initComponents();
        buildLayout();
    }

    private void loadBackgroundImage() {
        try {
            // First try loading from resources directory file
            File bgFile = new File("src/main/resources/faculty_home_banner.png");
            if (bgFile.exists()) {
                backgroundImage = ImageIO.read(bgFile);
            } else {
                // Try classpath
                InputStream is = getClass().getResourceAsStream("/faculty_home_banner.png");
                if (is != null) {
                    backgroundImage = ImageIO.read(is);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load login background image: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (backgroundImage != null) {
            // Scale and draw to cover full screen maintaining aspect ratio
            int imgW = backgroundImage.getWidth();
            int imgH = backgroundImage.getHeight();
            double scale = Math.max((double) width / imgW, (double) height / imgH);
            int drawW = (int) (imgW * scale);
            int drawH = (int) (imgH * scale);
            int drawX = (width - drawW) / 2;
            int drawY = (height - drawH) / 2;

            g2.drawImage(backgroundImage, drawX, drawY, drawW, drawH, null);

            // Sleek cinematic gradient overlay: Deep navy tint to make text & card pop
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(18, 59, 93, 140),
                0, height, new Color(10, 30, 50, 190)
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, width, height);
        } else {
            // Fallback gradient background
            GradientPaint gradient = new GradientPaint(
                0, 0, Theme.PRIMARY,
                width, height, Theme.SECONDARY
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, width, height);
        }

        g2.dispose();
    }

    private void initComponents() {
        usernameField = new CustomTextField("Enter your username / ID", 18);
        passwordField = new CustomPasswordField("Enter your password", 18);
        loginButton = new CustomButton("Sign In to AMS", CustomButton.ButtonType.PRIMARY);
        loginButton.setPreferredSize(new Dimension(320, 42));

        // Error Banner
        errorBanner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ERROR_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Theme.ERROR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        errorBanner.setOpaque(false);
        errorBanner.setVisible(false);

        errorLabel = new JLabel("");
        errorLabel.setFont(Theme.FONT_SMALL_BOLD);
        errorLabel.setForeground(Theme.ERROR);
        errorBanner.add(errorLabel);

        loadingLabel = new JLabel("Verifying credentials with central server...", SwingConstants.CENTER);
        loadingLabel.setFont(Theme.FONT_SMALL);
        loadingLabel.setForeground(Theme.TEXT_MUTED);
        loadingLabel.setVisible(false);

        // Enter key listeners
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        };
        usernameField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);
    }

    private void buildLayout() {
        // Modern frosted card container
        CardPanel card = new CardPanel(new BorderLayout(0, 18)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Crisp white semi-translucent glass effect
                g2.setColor(new Color(255, 255, 255, 248));
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                // Subtle gold/blue glow border
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(450, 570));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        // Header Section
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // University Emblem Badge
        JPanel emblem = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
                g2.dispose();
            }
        };
        emblem.setOpaque(false);
        emblem.setPreferredSize(new Dimension(54, 54));
        emblem.setMaximumSize(new Dimension(54, 54));
        emblem.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel emblemText = new JLabel("AMS", SwingConstants.CENTER);
        emblemText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        emblemText.setForeground(Theme.ACCENT);
        emblem.setLayout(new BorderLayout());
        emblem.add(emblemText, BorderLayout.CENTER);

        JLabel title = new JLabel("Faculty of Technology");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Theme.PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel uniTitle = new JLabel("UNIVERSITY OF RUHUNA");
        uniTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        uniTitle.setForeground(Theme.SECONDARY);
        uniTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Academic Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(Theme.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(emblem);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(title);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        headerPanel.add(uniTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        headerPanel.add(subtitle);

        // Form Section
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);

        // Error message banner
        errorBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        formPanel.add(errorBanner);
        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Username
        JLabel uLabel = new JLabel("Username / ID");
        uLabel.setFont(Theme.FONT_BOLD);
        uLabel.setForeground(Theme.TEXT_PRIMARY);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        formPanel.add(uLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        formPanel.add(usernameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        // Password
        JLabel pLabel = new JLabel("Password");
        pLabel.setFont(Theme.FONT_BOLD);
        pLabel.setForeground(Theme.TEXT_PRIMARY);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        formPanel.add(pLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        // Login Button
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        formPanel.add(loginButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        // Loading
        loadingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(loadingLabel);

        // Demo test accounts quick switcher
        JPanel quickDemo = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        quickDemo.setOpaque(false);
        JLabel demoLbl = new JLabel("Quick Demo: ");
        demoLbl.setFont(Theme.FONT_SMALL);
        demoLbl.setForeground(Theme.TEXT_MUTED);
        quickDemo.add(demoLbl);

        String[][] accounts = {
            {"Admin", "admin", "admin123"},
            {"Lecturer", "lecturer", "lecturer123"},
            {"Tech Officer", "techofficer", "tech123"},
            {"Student", "student", "student123"}
        };

        for (String[] acc : accounts) {
            JButton chip = new JButton(acc[0]);
            chip.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            chip.setForeground(Theme.PRIMARY);
            chip.setBackground(Theme.PRIMARY_LIGHT);
            chip.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            chip.setFocusPainted(false);
            chip.setCursor(new Cursor(Cursor.HAND_CURSOR));
            chip.addActionListener(e -> {
                usernameField.setText(acc[1]);
                passwordField.setText(acc[2]);
            });
            quickDemo.add(chip);
        }
        quickDemo.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        formPanel.add(quickDemo);

        // Footer Section
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);
        JLabel footerNote = new JLabel("University Academic Affairs | Secured Gateway");
        footerNote.setFont(Theme.FONT_SMALL);
        footerNote.setForeground(Theme.TEXT_MUTED);
        footerPanel.add(footerNote);

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);
        card.add(footerPanel, BorderLayout.SOUTH);

        add(card);
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void showError(String message) {
        errorLabel.setText("⚠ " + message);
        errorBanner.setVisible(true);
        revalidate();
        repaint();
    }

    public void clearError() {
        errorLabel.setText("");
        errorBanner.setVisible(false);
        revalidate();
        repaint();
    }

    public void setLoading(boolean isLoading) {
        loadingLabel.setVisible(isLoading);
        loginButton.setEnabled(!isLoading);
        usernameField.setEnabled(!isLoading);
        passwordField.setEnabled(!isLoading);
    }
}
