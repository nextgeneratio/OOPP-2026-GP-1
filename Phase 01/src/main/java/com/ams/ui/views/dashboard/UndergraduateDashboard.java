package com.ams.ui.views.dashboard;

import com.ams.ui.components.CardPanel;
import com.ams.ui.components.CustomBadge;
import com.ams.ui.components.CustomButton;
import com.ams.ui.components.CustomTable;
import com.ams.ui.components.StatCard;
import com.ams.ui.theme.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UndergraduateDashboard extends JPanel {

    public UndergraduateDashboard() {
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header Title Banner: Student info
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel pageTitle = new JLabel("Student Academic Portal");
        pageTitle.setFont(Theme.FONT_TITLE_LARGE);
        pageTitle.setForeground(Theme.PRIMARY);

        JLabel subtitle = new JLabel("Alice Johnson • Reg No: TG/2022/894 • Dept. of Information & Communication Technology");
        subtitle.setFont(Theme.FONT_REGULAR);
        subtitle.setForeground(Theme.TEXT_SECONDARY);

        titleBlock.add(pageTitle);
        titleBlock.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBlock.add(subtitle);

        JPanel badgeBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        badgeBlock.setOpaque(false);
        badgeBlock.add(CustomBadge.success("ACADEMIC STANDING: GOOD"));
        badgeBlock.add(CustomBadge.primary("YEAR 3 • SEMESTER 1"));

        headerPanel.add(titleBlock, BorderLayout.WEST);
        headerPanel.add(badgeBlock, BorderLayout.EAST);
        content.add(headerPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Stat Cards Grid: Academic Performance & CGPA
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        statsPanel.setOpaque(false);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        statsPanel.add(new StatCard("Cumulative GPA (CGPA)", "3.68", "Class Distinction Pace", Theme.PRIMARY, "GPA"));
        statsPanel.add(new StatCard("Previous Semester SGPA", "3.75", "Year 2 Semester 2", Theme.SECONDARY, "SGP"));
        statsPanel.add(new StatCard("Enrolled Credits", "16 Credits", "5 Courses This Sem", Theme.ACCENT, "CRD"));
        statsPanel.add(new StatCard("Exam Eligibility", "Eligible (5/5)", "80% Attendance Met", Theme.SUCCESS, "ELG"));

        content.add(statsPanel);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Enrolled Courses & Attendance Status Table
        CardPanel coursesCard = new CardPanel(new BorderLayout(0, 12));

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);

        JLabel coursesTitle = new JLabel("Enrolled Courses, Attendance & Exam Eligibility");
        coursesTitle.setFont(Theme.FONT_SUBTITLE);
        coursesTitle.setForeground(Theme.TEXT_PRIMARY);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);
        headerActions.add(new CustomButton("Course Timetable", CustomButton.ButtonType.OUTLINE));
        headerActions.add(new CustomButton("View Full Grades History", CustomButton.ButtonType.PRIMARY));

        cardHeader.add(coursesTitle, BorderLayout.WEST);
        cardHeader.add(headerActions, BorderLayout.EAST);

        String[] columns = {"Course Code", "Course Title", "Credits", "Lec Attendance %", "Lab Attendance %", "CA Marks (40%)", "Exam Eligibility"};
        Object[][] data = {
            {"ICT 3113", "Advanced Database Systems", "3", "92% (23/25)", "96% (24/25)", "36.5 / 40", "Eligible"},
            {"ICT 3123", "Software Engineering & Architecture", "3", "88% (22/25)", "90% (9/10)", "34.0 / 40", "Eligible"},
            {"ICT 3132", "Computer Networks & Security", "2", "84% (21/25)", "85% (17/20)", "32.0 / 40", "Eligible"},
            {"ICT 3143", "Web Application Development", "3", "96% (24/25)", "100% (10/10)", "38.0 / 40", "Eligible"},
            {"ENG 3102", "Technical Communication Skills", "2", "80% (20/25)", "N/A", "28.5 / 40", "Eligible"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        CustomTable table = new CustomTable(model);
        
        // Custom renderer for eligibility column
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                lbl.setFont(Theme.FONT_BOLD);
                lbl.setForeground(Theme.SUCCESS);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return lbl;
            }
        });

        JScrollPane scrollPane = CustomTable.createScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 210));

        coursesCard.add(cardHeader, BorderLayout.NORTH);
        coursesCard.add(scrollPane, BorderLayout.CENTER);
        content.add(coursesCard);
        content.add(Box.createRigidArea(new Dimension(0, 20)));

        // Recent Notices and Timetable Split
        JPanel bottomSplit = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomSplit.setOpaque(false);

        // Academic Notices Card
        CardPanel noticesCard = new CardPanel(new BorderLayout(0, 10));
        JLabel noticesTitle = new JLabel("Announcements & Exam Registration");
        noticesTitle.setFont(Theme.FONT_SUBTITLE);
        noticesCard.add(noticesTitle, BorderLayout.NORTH);

        JPanel noticeList = new JPanel();
        noticeList.setLayout(new BoxLayout(noticeList, BoxLayout.Y_AXIS));
        noticeList.setOpaque(false);

        String[][] notices = {
            {"End-Semester Exam Hall Admissions Now Available", "Download and print admission card before Nov 01, 2026"},
            {"Deadline for Medical Certificate Submission", "All pending medical submissions must be handed to TO by Oct 28"},
            {"ICT 3113: Guest Lecture on Distributed Storage", "Scheduled for Friday Oct 16 at Mini Auditorium"}
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

        // Upcoming Timetable Card
        CardPanel timeCard = new CardPanel(new BorderLayout(0, 10));
        JLabel timeTitle = new JLabel("Upcoming Classes (Tomorrow)");
        timeTitle.setFont(Theme.FONT_SUBTITLE);
        timeCard.add(timeTitle, BorderLayout.NORTH);

        String[] schedCols = {"Time", "Course Code", "Venue / Hall", "Type"};
        Object[][] schedData = {
            {"08:30 - 10:30", "ICT 3113", "Software Lab 01", "Practical"},
            {"10:30 - 12:30", "ICT 3143", "Hall 4A", "Lecture"},
            {"13:30 - 15:30", "ICT 3123", "Hall 2B", "Lecture"}
        };

        DefaultTableModel schedModel = new DefaultTableModel(schedData, schedCols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        CustomTable schedTable = new CustomTable(schedModel);
        JScrollPane schedScroll = CustomTable.createScrollPane(schedTable);
        schedScroll.setPreferredSize(new Dimension(0, 140));

        timeCard.add(schedScroll, BorderLayout.CENTER);

        bottomSplit.add(noticesCard);
        bottomSplit.add(timeCard);
        content.add(bottomSplit);

        JScrollPane mainScrollPane = new JScrollPane(content);
        mainScrollPane.setBorder(null);
        mainScrollPane.setOpaque(false);
        mainScrollPane.getViewport().setOpaque(false);

        add(mainScrollPane, BorderLayout.CENTER);
    }
}
