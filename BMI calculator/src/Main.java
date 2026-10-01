import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main extends JFrame {

    private JTextField weightField;
    private JComboBox<String> weightUnitCombo;
    private JTextField heightField;
    private JComboBox<String> heightUnitCombo;
    private JButton calculateButton;
    private JButton clearButton;
    private JLabel bmiScoreLabel;
    private JLabel bmiCategoryLabel;

    public Main() {
        setTitle("BMI CALCULATOR");
        setSize(500, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));

        JLabel titleLabel = new JLabel("BMI Calculator", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));

        headerPanel.add(titleLabel);
        container.add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.gridwidth = 2;
        JLabel wLabel = new JLabel("Weight");
        wLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(wLabel, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.7;
        weightField = new JTextField();
        styleTextField(weightField);
        formPanel.add(weightField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(12, 10, 12, 0);
        weightUnitCombo = new JComboBox<>(new String[]{"kg", "pound"});
        styleComboBox(weightUnitCombo);
        formPanel.add(weightUnitCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 1.0; gbc.gridwidth = 2;
        gbc.insets = new Insets(12, 0, 12, 0);
        JLabel hLabel = new JLabel("Height");
        hLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(hLabel, gbc);

        gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.7;
        heightField = new JTextField();
        styleTextField(heightField);
        formPanel.add(heightField, gbc);

        gbc.gridx = 1; gbc.weightx = 0.3;
        gbc.insets = new Insets(12, 10, 12, 0);
        heightUnitCombo = new JComboBox<>(new String[]{"cm", "In"});
        styleComboBox(heightUnitCombo);
        formPanel.add(heightUnitCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.insets = new Insets(25, 0, 10, 0);
        calculateButton = new JButton("Calculate BMI");
        calculateButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        calculateButton.setFocusPainted(false);
        calculateButton.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        calculateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(calculateButton, gbc);

        container.add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));

        JLabel resHeader = new JLabel("Result", SwingConstants.LEFT);
        resHeader.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottomPanel.add(resHeader, BorderLayout.NORTH);

        JPanel resultCard = new JPanel(new GridLayout(1, 2, 15, 0));
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JPanel scoreBox = new JPanel(new GridLayout(2, 1));
        JLabel scoreTitle = new JLabel("BMI SCORE", SwingConstants.CENTER);
        scoreTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bmiScoreLabel = new JLabel("--.-", SwingConstants.CENTER);
        bmiScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        scoreBox.add(scoreTitle);
        scoreBox.add(bmiScoreLabel);

        JPanel categoryBox = new JPanel(new GridLayout(2, 1));
        JLabel catTitle = new JLabel("CATEGORY", SwingConstants.CENTER);
        catTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bmiCategoryLabel = new JLabel("No Input", SwingConstants.CENTER);
        bmiCategoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        categoryBox.add(catTitle);
        categoryBox.add(bmiCategoryLabel);

        resultCard.add(scoreBox);
        resultCard.add(categoryBox);
        bottomPanel.add(resultCard, BorderLayout.CENTER);

        JPanel actionFooterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearButton.setFocusPainted(false);
        clearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        actionFooterPanel.add(clearButton);

        bottomPanel.add(actionFooterPanel, BorderLayout.SOUTH);

        container.add(bottomPanel, BorderLayout.SOUTH);
        setContentPane(container);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateBMI();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void styleComboBox(JComboBox<String> cb) {
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }

    private void calculateBMI() {
        try {
            double weight = Double.parseDouble(weightField.getText().trim());
            double height = Double.parseDouble(heightField.getText().trim());

            String weightUnit = (String) weightUnitCombo.getSelectedItem();
            String heightUnit = (String) heightUnitCombo.getSelectedItem();

            if (height <= 0 || weight <= 0) {
                JOptionPane.showMessageDialog(this, "Height and Weight must be greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double bmi = 0;

            if ("pound".equalsIgnoreCase(weightUnit) && "In".equalsIgnoreCase(heightUnit)) {
                bmi = (weight * 703) / (height * height);
            }
            else {
                double weightInKg = weight;
                double heightInMeters = height;

                if ("pound".equalsIgnoreCase(weightUnit)) {
                    weightInKg = weight * 0.45359237;
                }

                if ("cm".equalsIgnoreCase(heightUnit)) {
                    heightInMeters = height / 100.0;
                } else if ("In".equalsIgnoreCase(heightUnit)) {
                    heightInMeters = height * 0.0254;
                }

                bmi = weightInKg / (heightInMeters * heightInMeters);
            }

            bmiScoreLabel.setText(String.format("%.1f", bmi));

            if (bmi < 18.5) {
                bmiCategoryLabel.setText("Underweight");
            } else if (bmi >= 18.5 && bmi < 25.0) {
                bmiCategoryLabel.setText("Normal");
            } else if (bmi >= 25.0 && bmi < 30.0) {
                bmiCategoryLabel.setText("Overweight");
            } else {
                bmiCategoryLabel.setText("Obese");
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        weightField.setText("");
        heightField.setText("");
        weightUnitCombo.setSelectedIndex(0);
        heightUnitCombo.setSelectedIndex(0);
        bmiScoreLabel.setText("--.-");
        bmiCategoryLabel.setText("No Input");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main().setVisible(true);
            }
        });
    }
}
