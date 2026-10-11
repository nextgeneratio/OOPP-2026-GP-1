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

public class TechOfficerDashboard extends JPanel {

    public TechOfficerDashboard() {
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

        JLabel pageTitle = new JLabel("Technical Officer Portal");
        pageTitle.setFont(Theme.FONT_TITLE_LARGE);
        pageTitle.setForeground(Theme.PRIMARY);

        JLabel subtitle = new JLabel("Welcome Mr. John Doe • Department of Information & Communication Technology (Lab Ops)");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(pageTitle);
        titleBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBlock.add(subtitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        badgeBlock.setOpaque(false);
        badgeBlock.add(CustomBadge.info("LAB WORKSTATIONS: 45/45 READY"));
        badgeBlock.add(CustomBadge.primary("SEMESTER 1"));

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(badgeBlock, BorderLayout.EAST);
        content.add(headerPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Stat Cards Grid
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        statsPanel.add(new StatCard("Today's Practical Sessions", "4 Sessions", "2 Completed / 2 Pending", Theme.PRIMARY, "LAB"));
        statsPanel.add(new StatCard("Pending Medical Records", "6 Submissions", "Requires Verification", Theme.WARNING, "MED"));
        statsPanel.add(new StatCard("Attendance Logging", "98.2%", "Current Semester Average", Theme.SUCCESS, "LOG"));
        statsPanel.add(new StatCard("Lab Timetable Slots", "18 Hours/Wk", "No Schedule Conflicts", Theme.SECONDARY, "TTB"));

        content.add(statsPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Pending Medical Approvals Card
        CardPanel medicalCard = new CardPanel(new BorderLayout(0, 12));

        JPanel medHeader = new JPanel(new BorderLayout());
        medHeader.setOpaque(false);

        JLabel medTitle = new JLabel("Pending Medical Certificates Awaiting Review");
        medTitle.setFont(Theme.FONT_SUBTITLE);
        medTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel medActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        medActions.setOpaque(false);
        medActions.add(new CustomButton("View All Medicals", CustomButton.ButtonType.OUTLINE));
        medActions.add(new CustomButton("Log New Attendance", CustomButton.ButtonType.PRIMARY));

        medHeader.add(medTitle, BorderLayout.WEST);
        medHeader.add(medActions, BorderLayout.EAST);

        String[] columns = {"Submission ID", "Student Reg No", "Student Name", "Course Missed", "Medical Period", "Hospital / Doctor", "Verification"};
        Object[][] data = {
            {"MED-2026-081", "TG/2022/104", "K. D. Fernando", "ICT 3113 Practical", "Oct 02 - Oct 04, 2026", "General Hospital Colombo", "Pending Review"},
            {"MED-2026-082", "TG/2023/215", "M. S. Senanayake", "ICT 2123 Lab Session", "Oct 05 - Oct 06, 2026", "University Medical Center", "Pending Review"},
            {"MED-2026-083", "TG/2021/045", "S. A. Perera", "ICT 4142 Lecture", "Sep 28 - Sep 30, 2026", "Base Hospital Panadura", "Pending Review"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        CustomTable table = new CustomTable(model);
        JScrollPane scrollPane = CustomTable.createScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 160));

        medicalCard.add(medHeader, BorderLayout.NORTH);
        medicalCard.add(scrollPane, BorderLayout.CENTER);
        content.add(medicalCard);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Today's Scheduled Lab Sessions
        CardPanel labScheduleCard = new CardPanel(new BorderLayout(0, 12));
        JLabel scheduleTitle = new JLabel("Today's Laboratory Timetable Schedule");
        scheduleTitle.setFont(Theme.FONT_SUBTITLE);
        labScheduleCard.add(scheduleTitle, BorderLayout.NORTH);

        String[] schedCols = {"Time Slot", "Laboratory", "Course & Group", "Lecturer in Charge", "Session Type", "Attendance State"};
        Object[][] schedData = {
            {"08:30 - 10:30", "Software Lab 01", "ICT 3113 - Batch 2022 (Group A)", "Dr. Jane Smith", "Practical / Hands-on", "Marked (62/65)"},
            {"10:30 - 12:30", "Software Lab 01", "ICT 3113 - Batch 2022 (Group B)", "Dr. Jane Smith", "Practical / Hands-on", "Marked (63/65)"},
            {"13:30 - 15:30", "Network Lab 02", "ICT 2123 - Batch 2023 (All)", "Mr. R. Bandara", "Practical Session", "Scheduled (Upcoming)"},
            {"15:30 - 17:30", "Hardware Lab 01", "ET 2102 - Batch 2023", "Dr. N. Wickramasinghe", "Experimental Workshop", "Scheduled (Upcoming)"}
        };

        DefaultTableModel schedModel = new DefaultTableModel(schedData, schedCols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        CustomTable schedTable = new CustomTable(schedModel);
        JScrollPane schedScroll = CustomTable.createScrollPane(schedTable);
        schedScroll.setPreferredSize(new Dimension(0, 180));

        labScheduleCard.add(schedScroll, BorderLayout.CENTER);
        content.add(labScheduleCard);

        JScrollPane mainScrollPane = new JScrollPane(content);
        mainScrollPane.setBorder(null);
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);

        add(mainScrollPane, BorderLayout.CENTER);
    }
}
