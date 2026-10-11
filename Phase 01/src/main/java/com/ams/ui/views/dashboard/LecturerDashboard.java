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

public class LecturerDashboard extends JPanel {

    public LecturerDashboard() {
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

        JLabel pageTitle = new JLabel("Lecturer Academic Dashboard");
        pageTitle.setFont(Theme.FONT_TITLE_LARGE);
        pageTitle.setForeground(Theme.PRIMARY);

        JLabel subtitle = new JLabel("Welcome Dr. Jane Smith • Senior Lecturer in Information & Communication Technology");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(pageTitle);
        titleBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBlock.add(subtitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        badgeBlock.setOpaque(false);
        badgeBlock.add(CustomBadge.warning("MARKS SUBMISSION OPEN"));
        badgeBlock.add(CustomBadge.primary("SEMESTER 1 • 2025/26"));

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(badgeBlock, BorderLayout.EAST);
        content.add(headerPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Stat Cards Grid
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        statsPanel.add(new StatCard("Assigned Courses", "3 Courses", "Active Offerings", Theme.PRIMARY, "CRS"));
        statsPanel.add(new StatCard("Total Undergraduates", "185 Students", "Across 3 Batches", Theme.SECONDARY, "STD"));
        statsPanel.add(new StatCard("Pending CA Marks", "2 Tasks", "Quiz 2 & Lab Final", Theme.WARNING, "MRK"));
        statsPanel.add(new StatCard("Attendance Status", "94% Avg", "Lab & Lectures Logged", Theme.SUCCESS, "ATT"));

        content.add(statsPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Assigned Courses Card
        CardPanel coursesCard = new CardPanel(new BorderLayout(0, 12));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel coursesTitle = new JLabel("My Assigned Course Offerings");
        coursesTitle.setFont(Theme.FONT_SUBTITLE);
        coursesTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(new CustomButton("Course Materials", CustomButton.ButtonType.OUTLINE));
        headerActions.add(new CustomButton("Enter Marks", CustomButton.ButtonType.PRIMARY));

        cardHeader.add(coursesTitle, BorderLayout.WEST);
        cardHeader.add(headerActions, BorderLayout.EAST);

        String[] columns = {"Course Code", "Course Title", "Credits", "Target Batch", "Students", "CA Weightage", "Status"};
        Object[][] data = {
            {"ICT 3113", "Advanced Database Systems", "3 (2L / 1P)", "Batch 2022 (Year 3)", "65", "40%", "In Progress"},
            {"ICT 2123", "Object-Oriented Programming (Java)", "3 (2L / 1P)", "Batch 2023 (Year 2)", "72", "40%", "In Progress"},
            {"ICT 4142", "Cloud Computing & DevOps", "2 (2L)", "Batch 2021 (Year 4)", "48", "30%", "In Progress"}
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

        coursesCard.add(cardHeader, BorderLayout.NORTH);
        coursesCard.add(scrollPane, BorderLayout.CENTER);
        content.add(coursesCard);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Bottom Split: Quick Actions & Recent Notices
        JPanel bottomSplit = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomSplit.setOpaque(false);

        // Assessment Tasks
        CardPanel tasksCard = new CardPanel(new BorderLayout(0, 10));
        JLabel tasksTitle = new JLabel("Pending Assessments & Deadlines");
        tasksTitle.setFont(Theme.FONT_SUBTITLE);
        tasksCard.add(tasksTitle, BorderLayout.NORTH);

        JPanel taskList = new JPanel();
        taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
        taskList.setOpaque(false);

        String[][] tasks = {
            {"ICT 3113: Lab Practical 02 Marks Entry", "Deadline: Oct 20, 2026", "Pending"},
            {"ICT 2123: Mid-Semester Quiz Results", "Deadline: Oct 25, 2026", "Pending"},
            {"ICT 4142: Course Material Upload (Week 6)", "Completed", "Done"}
        };

        for (String[] t : tasks) {
            JPanel item = new JPanel(new BorderLayout());
            item.setOpaque(false);
            item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 4, 6, 4)
            ));
            JLabel tName = new JLabel(t[0]);
            tName.setFont(Theme.FONT_REGULAR);
            JLabel tDue = new JLabel(t[1]);
            tDue.setFont(Theme.FONT_SMALL);
            tDue.setForeground(Theme.TEXT_MUTED);

            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setOpaque(false);
            left.add(tName);
            left.add(tDue);

            CustomBadge b = t[2].equals("Done") ? CustomBadge.success("COMPLETED") : CustomBadge.warning("ACTION REQUIRED");

            item.add(left, BorderLayout.WEST);
            item.add(b, BorderLayout.EAST);
            taskList.add(item);
        }
        tasksCard.add(taskList, BorderLayout.CENTER);

        // Notices
        CardPanel noticesCard = new CardPanel(new BorderLayout(0, 10));
        JLabel noticesTitle = new JLabel("Recent Department Notices");
        noticesTitle.setFont(Theme.FONT_SUBTITLE);
        noticesCard.add(noticesTitle, BorderLayout.NORTH);

        JPanel noticeList = new JPanel();
        noticeList.setLayout(new BoxLayout(noticeList, BoxLayout.Y_AXIS));
        noticeList.setOpaque(false);

        String[][] notices = {
            {"End-Semester Examination Timetable Draft Released", "Oct 08, 2026 • Examination Unit"},
            {"Submission of Continuous Assessment Marks for Sem 1", "Oct 05, 2026 • Dean's Office"},
            {"Laboratory Maintenance Schedule (Network Lab)", "Sep 29, 2026 • Tech Support"}
        };

        for (String[] n : notices) {
            JPanel item = new JPanel(new BorderLayout());
            item.setOpaque(false);
            item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 4, 6, 4)
            ));
            JLabel nTitle = new JLabel(n[0]);
            nTitle.setFont(Theme.FONT_REGULAR);
            JLabel nMeta = new JLabel(n[1]);
            nMeta.setFont(Theme.FONT_SMALL);
            nMeta.setForeground(Theme.TEXT_MUTED);

            JPanel block = new JPanel();
            block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
            block.setOpaque(false);
            block.add(nTitle);
            block.add(nMeta);

            item.add(block, BorderLayout.CENTER);
            noticeList.add(item);
        }
        noticesCard.add(noticeList, BorderLayout.CENTER);

        bottomSplit.add(tasksCard);
        bottomSplit.add(noticesCard);
        content.add(bottomSplit);

        JScrollPane mainScrollPane = new JScrollPane(content);
        mainScrollPane.setBorder(null);
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);

        add(mainScrollPane, BorderLayout.CENTER);
    }
}
