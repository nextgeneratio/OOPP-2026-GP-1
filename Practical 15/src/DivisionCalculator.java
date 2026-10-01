import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DivisionCalculator extends JFrame {
    private JTextField txtNum1;
    private JTextField txtNum2;
    private JLabel lblResultValue;

    public DivisionCalculator() {
        // Set up the frame
        setTitle("Division Calculator");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setLocationRelativeTo(null);

        // Number 1 Label and TextField
        JLabel lblNum1 = new JLabel("Number 1 :");
        lblNum1.setBounds(30, 20, 80, 25);
        add(lblNum1);

        txtNum1 = new JTextField();
        txtNum1.setBounds(110, 20, 60, 25);
        add(txtNum1);

        // Number 2 Label and TextField
        JLabel lblNum2 = new JLabel("Number 2 :");
        lblNum2.setBounds(30, 55, 80, 25);
        add(lblNum2);

        txtNum2 = new JTextField();
        txtNum2.setBounds(110, 55, 60, 25);
        add(txtNum2);

        // Result Static Label
        JLabel lblResult = new JLabel("Result");
        lblResult.setBounds(200, 20, 80, 25);
        add(lblResult);

        // Result Value Label
        lblResultValue = new JLabel("");
        lblResultValue.setBounds(200, 55, 80, 25);
        add(lblResultValue);

        // Divide Button
        JButton btnDivide = new JButton("Divide");
        btnDivide.setBounds(60, 100, 90, 30);
        add(btnDivide);

        // Add button click action listener
        btnDivide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateDivision();
            }
        });
    }

    private void calculateDivision() {
        try {
            double num1 = Double.parseDouble(txtNum1.getText().trim());
            double num2 = Double.parseDouble(txtNum2.getText().trim());

            if (num2 == 0) {
                // Clear the result area as per Figure B
                lblResultValue.setText("");

                // Show error message dialog as per Figure C
                JOptionPane.showMessageDialog(this,
                        "You can't enter 0 for second number",
                        "Message",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Perform division and display the result
                double result = num1 / num2;
                lblResultValue.setText(String.valueOf(result));
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numeric values.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Run the GUI application
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new DivisionCalculator().setVisible(true);
            }
        });
    }
}
