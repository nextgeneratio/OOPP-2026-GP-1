import javax.swing.*;
import java.awt.*;

public class NewUI extends JFrame {

    public NewUI() {
        setTitle("BMI Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 720);
        setLocationRelativeTo(null);
        setLayout(null);

        JLabel title = new JLabel("BMI Calculator", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBounds(10, 10, 700, 40);
        add(title);

        JLabel wLabel = new JLabel("Weight");
        wLabel.setBounds(100, 100, 100, 30);
        add(wLabel);

        JTextField weightField = new JTextField();
        weightField.setBounds(220, 100, 250, 30);
        add(weightField);

        JComboBox<String> weightCombo = new JComboBox<>(new String[]{"kg", "pound"});
        weightCombo.setBounds(480, 100, 100, 30);
        add(weightCombo);

        JLabel hLabel = new JLabel("Height");
        hLabel.setBounds(100, 150, 100, 30);
        add(hLabel);

        JTextField heightField = new JTextField();
        heightField.setBounds(220, 150, 250, 30);
        add(heightField);

        JComboBox<String> heightCombo = new JComboBox<>(new String[]{"m", "In"});
        heightCombo.setBounds(480, 150, 100, 30);
        add(heightCombo);

        JButton calculateButton = new JButton("Your BMI calculate");
        calculateButton.setBounds(100, 220, 480, 40);
        add(calculateButton);

        JLabel resultLabel = new JLabel("Result :-");
        resultLabel.setBounds(100, 300, 100, 30);
        add(resultLabel);

        JTextField bmiValueDisplay = new JTextField();
        bmiValueDisplay.setHorizontalAlignment(JTextField.CENTER);
        bmiValueDisplay.setEditable(false);
        bmiValueDisplay.setBounds(220, 300, 120, 30);
        add(bmiValueDisplay);

        JTextField bmiStatusDisplay = new JTextField();
        bmiStatusDisplay.setHorizontalAlignment(JTextField.CENTER);
        bmiStatusDisplay.setEditable(false);
        bmiStatusDisplay.setBounds(350, 300, 120, 30);
        add(bmiStatusDisplay);

        calculateButton.addActionListener(e -> {
            try {
                double weight = Double.parseDouble(weightField.getText());
                double height = Double.parseDouble(heightField.getText());

                if (weightCombo.getSelectedItem().equals("pound")) weight *= 0.45359237;
                if (heightCombo.getSelectedItem().equals("In")) height *= 0.0254;

                double bmi = weight / (height * height);
                int bmiInteger = (int) Math.round(bmi);
                bmiValueDisplay.setText(String.valueOf(bmiInteger));

                String status = (bmi < 18.5) ? "Underweight" :
                        (bmi < 25.0) ? "Normal" :
                                (bmi < 30.0) ? "Overweight" : "Obese";
                bmiStatusDisplay.setText(status);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Please enter valid numbers!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        new NewUI();
    }
}
