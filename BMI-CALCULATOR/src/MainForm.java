import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainForm extends JFrame {

    private JTextField weightField;
    private JComboBox<String> weightUnitCombo;
    private JTextField heightField;
    private JComboBox<String> heightUnitCombo;
    private JButton calculateButton;
    private JLabel bmiScoreLabel;
    private JLabel bmiCategoryLabel;

    // Creative modern color palette
    private final Color COLOR_BG = new Color(245, 247, 250);       // Soft light grey/blue
    private final Color COLOR_PRIMARY = new Color(79, 70, 229);   // Indigo blue Accent
    private final Color COLOR_TEXT_DARK = new Color(31, 41, 55);  // Deep charcoal
    private final Color COLOR_TEXT_MUTED = new Color(107, 114, 128); // Slate gray

    public MainForm() {
        setTitle("BMI CALCULATOR");
        setSize(500, 600); // Optimized window geometry dimension for this specific layout
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Core layout container setup
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(COLOR_BG);
        container.setBorder(new EmptyBorder(30, 40, 30, 40)); // Generous breathing room padding

        // --- TOP SECTION: HEADER ---
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setBackground(COLOR_BG);

        JLabel titleLabel = new JLabel("BMI Calculator", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(COLOR_PRIMARY);

        JLabel subTitleLabel = new JLabel("Check your body mass index health status", SwingConstants.CENTER);
        subTitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subTitleLabel.setForeground(COLOR_TEXT_MUTED);

        headerPanel.add(titleLabel);
        headerPanel.add(subTitleLabel);
        container.add(headerPanel, BorderLayout.NORTH);

        // --- MIDDLE SECTION: FORM INPUTS ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 0); // Equal padding space between row blocks
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Weight elements block setup
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.gridwidth = 2;
        JLabel wLabel = new JLabel("Weight");
        wLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        wLabel.setForeground(COLOR_TEXT_DARK);
        formPanel.add(wLabel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.7;
        weightField = new JTextField();
        styleTextField(weightField);
        formPanel.add(weightField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(12, 10, 12, 0); // Adds horizontal gap to separate inputs
        weightUnitCombo = new JComboBox<>(new String[]{"kg", "pound"});
        styleComboBox(weightUnitCombo);
        formPanel.add(weightUnitCombo, gbc);

        // Height elements block setup
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1.0; gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 0, 12, 0);
        JLabel hLabel = new JLabel("Height");
        hLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hLabel.setForeground(COLOR_TEXT_DARK);
        formPanel.add(hLabel, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.7;
        heightField = new JTextField();
        styleTextField(heightField);
        formPanel.add(heightField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(12, 10, 12, 0);
        heightUnitCombo = new JComboBox<>(new String[]{"m", "In"});
        styleComboBox(heightUnitCombo);
        formPanel.add(heightUnitCombo, gbc);

        // Action submit button element setup
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(25, 0, 10, 0);
        calculateButton = new JButton("Calculate BMI");
        calculateButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setBackground(COLOR_PRIMARY);
        calculateButton.setFocusPainted(false);
        calculateButton.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        calculateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(calculateButton, gbc);

        container.add(formPanel, BorderLayout.CENTER);

        // --- BOTTOM SECTION: CREATIVE RESULTS CARD ---
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setBackground(COLOR_BG);

        JLabel resHeader = new JLabel("Result", SwingConstants.LEFT);
        resHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        resHeader.setForeground(COLOR_TEXT_DARK);
        bottomPanel.add(resHeader, BorderLayout.NORTH);

        JPanel resultCard = new JPanel(new GridLayout(1, 2, 15, 0));
        resultCard.setBackground(Color.WHITE);
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel scoreBox = new JPanel(new GridLayout(2, 1));
        scoreBox.setBackground(Color.WHITE);
        JLabel scoreTitle = new JLabel("BMI SCORE", SwingConstants.CENTER);
        scoreTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        scoreTitle.setForeground(COLOR_TEXT_MUTED);
        bmiScoreLabel = new JLabel("--.-", SwingConstants.CENTER);
        bmiScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        bmiScoreLabel.setForeground(COLOR_TEXT_DARK);
        scoreBox.add(scoreTitle);
        scoreBox.add(bmiScoreLabel);

        JPanel categoryBox = new JPanel(new GridLayout(2, 1));
        categoryBox.setBackground(Color.WHITE);
        JLabel catTitle = new JLabel("CATEGORY", SwingConstants.CENTER);
        catTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        catTitle.setForeground(COLOR_TEXT_MUTED);
        bmiCategoryLabel = new JLabel("No Input", SwingConstants.CENTER);
        bmiCategoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        bmiCategoryLabel.setForeground(COLOR_TEXT_MUTED);
        categoryBox.add(catTitle);
        categoryBox.add(bmiCategoryLabel);

        resultCard.add(scoreBox);
        resultCard.add(categoryBox);
        bottomPanel.add(resultCard, BorderLayout.CENTER);

        container.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(container);

        // Interaction logic trigger link connection
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateBMI();
            }
        });
    }

    // Modern flat element helper design functions
    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        tf.setBackground(Color.WHITE);
        tf.setForeground(COLOR_TEXT_DARK);
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setBackground(Color.WHITE);
        cb.setForeground(COLOR_TEXT_DARK);
    }

    private void calculateBMI() {
        try {
            double weight = Double.parseDouble(weightField.getText().trim());
            double height = Double.parseDouble(heightField.getText().trim());

            String weightUnit = (String) weightUnitCombo.getSelectedItem();
            String heightUnit = (String) heightUnitCombo.getSelectedItem();

            if ("pound".equalsIgnoreCase(weightUnit)) weight = weight * 0.45359237;
            if ("In".equalsIgnoreCase(heightUnit)) height = height * 0.0254;

            if (height <= 0) {
                JOptionPane.showMessageDialog(this, "Height must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double bmi = weight / (height * height);
            bmiScoreLabel.setText(String.format("%.1f", bmi));

            // Dynamic color coding styling depending on classification output rules
            String category;
            Color categoryColor;

            if (bmi < 18.5) {
                category = "Underweight";
                categoryColor = new Color(245, 158, 11); // Amber orange warning
            } else if (bmi < 25.0) {
                category = "Normal";
                categoryColor = new Color(16, 185, 129); // Vibrant healthy green
            } else if (bmi < 30.0) {
                category = "Overweight";
                categoryColor = new Color(239, 68, 68);  // Warning soft red
            } else {
                category = "Obese";
                categoryColor = new Color(153, 27, 27);  // High warning deep blood crimson
            }

            bmiCategoryLabel.setText(category);
            bmiCategoryLabel.setForeground(categoryColor);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainForm().setVisible(true));
    }
}