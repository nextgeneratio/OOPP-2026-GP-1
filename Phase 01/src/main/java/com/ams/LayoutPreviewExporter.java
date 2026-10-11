package com.ams;

import com.ams.model.User;
import com.ams.ui.shared.MainFrame;
import com.ams.ui.theme.Theme;
import com.ams.ui.views.LoginView;
import com.ams.ui.views.dashboard.AdminDashboard;
import com.ams.ui.views.dashboard.LecturerDashboard;
import com.ams.ui.views.dashboard.TechOfficerDashboard;
import com.ams.ui.views.dashboard.UndergraduateDashboard;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class LayoutPreviewExporter {

    public static void main(String[] args) {
        // Run in headless or Swing thread
        SwingUtilities.invokeLater(() -> {
            try {
                exportAllLayouts();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void exportAllLayouts() throws Exception {
        Theme.setupUIManager();

        File outputDir = new File("previews");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // 1. Export Login Screen
        exportLoginPreview(new File(outputDir, "01_login_view.png"));

        // 2. Export Admin Dashboard
        exportRolePreview("Admin", "System Admin", 
            new AdminDashboard(), 
            Arrays.asList("Dashboard", "Users", "Courses", "Timetable", "Reports"), 
            new File(outputDir, "02_admin_dashboard.png"));

        // 3. Export Lecturer Dashboard
        exportRolePreview("Lecturer", "Dr. Jane Smith", 
            new LecturerDashboard(), 
            Arrays.asList("Dashboard", "My Courses", "Attendance", "Marks", "Notices"), 
            new File(outputDir, "03_lecturer_dashboard.png"));

        // 4. Export Technical Officer Dashboard
        exportRolePreview("Tech Officer", "Mr. John Doe", 
            new TechOfficerDashboard(), 
            Arrays.asList("Dashboard", "Attendance", "Medical Approvals", "Timetable"), 
            new File(outputDir, "04_tech_officer_dashboard.png"));

        // 5. Export Undergraduate Dashboard
        exportRolePreview("Undergraduate", "Alice Johnson", 
            new UndergraduateDashboard(), 
            Arrays.asList("Dashboard", "My Courses", "Eligibility", "Grades", "Timetable"), 
            new File(outputDir, "05_undergraduate_dashboard.png"));

        System.out.println("All layout PNG previews generated successfully in: " + outputDir.getAbsolutePath());
        System.exit(0);
    }

    private static void exportLoginPreview(File destination) throws Exception {
        int width = 1280;
        int height = 820;

        MainFrame frame = new MainFrame();
        frame.setSize(width, height);
        frame.setTopBarVisible(false);
        frame.setNavigationVisible(false);
        frame.getContentPane().remove(frame.getContentPane().getComponent(2)); // remove statusBar if needed or leave clean
        frame.setContent(new LoginView());
        frame.addNotify();
        frame.validate();

        renderComponentToPng(frame.getContentPane(), width, height, destination);
    }

    private static void exportRolePreview(String role, String userName, JPanel dashboard, java.util.List<String> navOptions, File destination) throws Exception {
        int width = 1280;
        int height = 820;

        MainFrame frame = new MainFrame();
        frame.setSize(width, height);
        frame.getTopBar().setUserInfo(userName, role);
        frame.getNavigationPanel().setMenuOptions(navOptions);
        frame.setContent(dashboard);
        frame.addNotify();
        frame.validate();

        renderComponentToPng(frame.getContentPane(), width, height, destination);
    }

    private static void renderComponentToPng(Container component, int width, int height, File file) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();

        g2.setColor(Theme.BACKGROUND);
        g2.fillRect(0, 0, width, height);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        component.setSize(width, height);
        layoutRecursively(component);
        component.printAll(g2);
        g2.dispose();

        ImageIO.write(image, "PNG", file);
        System.out.println("Exported: " + file.getName() + " (" + file.length() + " bytes)");
    }

    private static void layoutRecursively(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                layoutRecursively((Container) child);
            }
        }
    }
}
