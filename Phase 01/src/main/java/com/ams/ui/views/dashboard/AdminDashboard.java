package com.ams.ui.views.dashboard;

import com.ams.ui.components.CardPanel;
import com.ams.ui.components.CustomBadge;
import com.ams.ui.components.CustomButton;
import com.ams.ui.components.CustomTable;
import com.ams.ui.components.StatCard;
import com.ams.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AdminDashboard extends JPanel {

    public AdminDashboard() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header Title Banner
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel pageTitle = new JLabel("Administrator Dashboard");
        pageTitle.setFont(Theme.FONT_TITLE_LARGE);
        pageTitle.setForeground(Theme.PRIMARY);

        JLabel subtitle = new JLabel("System overview, academic operations, and administrative shortcuts.");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(pageTitle);
        titleBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBlock.add(subtitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        badgeBlock.setOpaque(false);
        badgeBlock.add(CustomBadge.success("SYSTEM STABLE"));
        badgeBlock.add(CustomBadge.primary("ACADEMIC YEAR 2025/26"));

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(badgeBlock, BorderLayout.EAST);
        content.add(headerPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Stat Cards Grid
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        statsPanel.add(new StatCard("System Users", "1,420", "Active Accounts", Theme.PRIMARY, "USR"));
        statsPanel.add(new StatCard("Departments", "4 Depts", "8 Active Batches", Theme.SECONDARY, "DPT"));
        statsPanel.add(new StatCard("Course Offerings", "96", "48 Unique Courses", Theme.ACCENT, "CRS"));
        statsPanel.add(new StatCard("Timetable Status", "Active", "0 Conflicts Detected", Theme.SUCCESS, "TTB"));

        content.add(statsPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Middle Section: Quick Actions Card
        CardPanel actionsCard = new CardPanel(new BorderLayout(0, 12));
        actionsCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel actionsTitle = new JLabel("Administrative Shortcuts");
        actionsTitle.setFont(Theme.FONT_SUBTITLE);
        actionsTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(new CustomButton("User Management", CustomButton.ButtonType.PRIMARY));
        buttonsPanel.add(new CustomButton("Student Management", CustomButton.ButtonType.PRIMARY));
        buttonsPanel.add(new CustomButton("Course Management", CustomButton.ButtonType.SECONDARY));
        buttonsPanel.add(new CustomButton("Timetable Scheduler", CustomButton.ButtonType.OUTLINE));
        buttonsPanel.add(new CustomButton("Publish Notice", CustomButton.ButtonType.OUTLINE));
        buttonsPanel.add(new CustomButton("Generate Reports", CustomButton.ButtonType.OUTLINE));

        actionsCard.add(actionsTitle, BorderLayout.NORTH);
        actionsCard.add(buttonsPanel, BorderLayout.CENTER);
        content.add(actionsCard);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Bottom Section: Academic Departments Overview Table
        CardPanel tableCard = new CardPanel(new BorderLayout(0, 12));

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel tableTitle = new JLabel("Department & Academic Batch Master Overview");
        tableTitle.setFont(Theme.FONT_SUBTITLE);
        tableTitle.setForeground(Theme.TEXT_PRIMARY);

        JLabel tableNote = new JLabel("Showing registered departments in Faculty of Technology");
        tableNote.setFont(Theme.FONT_SMALL);
        tableNote.setForeground(Theme.TEXT_MUTED);

        tableHeader.add(tableTitle, BorderLayout.WEST);
        tableHeader.add(tableNote, BorderLayout.EAST);

        String[] columns = {"Dept Code", "Department Name", "Head of Department", "Active Batches", "Undergraduates", "Status"};
        Object[][] data = {
            {"BST", "Bio Systems Technology", "Prof. K. A. Perera", "2021, 2022, 2023", "380", "Active"},
            {"ICT", "Information & Communication Technology", "Dr. Jane Smith", "2021, 2022, 2023", "420", "Active"},
            {"ET", "Engineering Technology", "Dr. N. Wickramasinghe", "2021, 2022, 2023", "350", "Active"},
            {"FST", "Food Science & Technology", "Prof. M. Jayasuriya", "2021, 2022, 2023", "270", "Active"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        CustomTable table = new CustomTable(model);
        JScrollPane scrollPane = CustomTable.createScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 190));

        tableCard.add(tableHeader, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        content.add(tableCard);

        JScrollPane mainScrollPane = new JScrollPane(content);
        mainScrollPane.setBorder(null);
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);

        add(mainScrollPane, BorderLayout.CENTER);
    }
}
