package com.ams.ui.components;

import javax.swing.*;
import java.awt.*;

public class DialogManager {
    public static void showError(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static boolean confirmAction(Component parent, String title, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
