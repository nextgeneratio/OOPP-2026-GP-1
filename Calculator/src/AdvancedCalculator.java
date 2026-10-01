import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Stack;

public class AdvancedCalculator extends JFrame {
    private JTextField displayField;
    private ArrayList<String> calculationHistory = new ArrayList<>();
    private boolean startNewInput = true;

    public AdvancedCalculator() {
        // Window setup
        setTitle("Swing Calculator");
        setSize(350, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        // Top Panel: Contains Display and the History Button requested in Part (b)
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        displayField = new JTextField("0");
        displayField.setFont(new Font("Arial", Font.PLAIN, 28));
        displayField.setHorizontalAlignment(JTextField.RIGHT);
        displayField.setEditable(false);
        displayField.setBackground(new Color(240, 240, 240));
        topPanel.add(displayField, BorderLayout.CENTER);

        JButton btnHistory = new JButton("History");
        btnHistory.setFont(new Font("Arial", Font.BOLD, 12));
        btnHistory.setFocusable(false);
        btnHistory.addActionListener(e -> showHistory());
        topPanel.add(btnHistory, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Grid Panel for Buttons matching the arrangement in part (a)
        JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 8, 8));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        String[] buttons = {
                "C", "(", ")", "/",
                "7", "8", "9", "*",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "0", ".", "=", "%"
        };

        for (String text : buttons) {
            JButton button = new JButton(text);
            button.setFont(new Font("Arial", Font.BOLD, 20));
            button.setFocusable(false);
            button.addActionListener(new ButtonClickListener());
            buttonPanel.add(button);
        }

        add(buttonPanel, BorderLayout.CENTER);
    }

    private class ButtonClickListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            String currentText = displayField.getText();

            if (command.equals("C")) {
                displayField.setText("0");
                startNewInput = true;
            } else if (command.equals("=")) {
                try {
                    double result = evaluateExpression(currentText);
                    // Format output: remove trailing .0 for whole integers
                    String resultString = (result % 1 == 0) ? String.valueOf((long) result) : String.valueOf(result);

                    // Save to history list (Limit to last 10 records)
                    recordHistory(currentText + " = " + resultString);

                    displayField.setText(resultString);
                } catch (Exception ex) {
                    displayField.setText("Error");
                }
                startNewInput = true;
            } else {
                if (startNewInput || currentText.equals("0") || currentText.equals("Error")) {
                    displayField.setText(command);
                    startNewInput = false;
                } else {
                    displayField.setText(currentText + command);
                }
            }
        }
    }

    // Records the last 10 calculations dynamically
    private void recordHistory(String record) {
        if (calculationHistory.size() >= 10) {
            calculationHistory.remove(0); // Evict oldest
        }
        calculationHistory.add(record);
    }

    // Displays the history collection inside a clean alert dialog
    private void showHistory() {
        if (calculationHistory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No history available yet.", "Calculation History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder historyBuilder = new StringBuilder("Last 10 Calculations:\n\n");
        for (int i = 0; i < calculationHistory.size(); i++) {
            historyBuilder.append(i + 1).append(". ").append(calculationHistory.get(i)).append("\n");
        }

        JOptionPane.showMessageDialog(this, historyBuilder.toString(), "Calculation History", JOptionPane.INFORMATION_MESSAGE);
    }

    // Basic Standard Infix Math Parsing Engine supporting +, -, *, /, %, (, )
    private double evaluateExpression(String expression) {
        char[] tokens = expression.toCharArray();
        Stack<Double> values = new Stack<>();
        Stack<Character> ops = new Stack<>();

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i] == ' ') continue;

            if ((tokens[i] >= '0' && tokens[i] <= '9') || tokens[i] == '.') {
                StringBuilder sbuf = new StringBuilder();
                while (i < tokens.length && ((tokens[i] >= '0' && tokens[i] <= '9') || tokens[i] == '.')) {
                    sbuf.append(tokens[i++]);
                }
                i--;
                values.push(Double.parseDouble(sbuf.toString()));
            } else if (tokens[i] == '(') {
                ops.push(tokens[i]);
            } else if (tokens[i] == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                if (!ops.isEmpty()) ops.pop();
            } else if (tokens[i] == '+' || tokens[i] == '-' || tokens[i] == '*' || tokens[i] == '/' || tokens[i] == '%') {
                while (!ops.isEmpty() && hasPrecedence(tokens[i], ops.peek())) {
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                }
                ops.push(tokens[i]);
            }
        }

        while (!ops.isEmpty()) {
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));
        }

        return values.isEmpty() ? 0 : values.pop();
    }

    private boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') return false;
        if ((op1 == '*' || op1 == '/' || op1 == '%') && (op2 == '+' || op2 == '-')) return false;
        return true;
    }

    private double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new UnsupportedOperationException("Cannot divide by zero");
                return a / b;
            case '%': return a % b;
        }
        return 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdvancedCalculator().setVisible(true));
    }
}
