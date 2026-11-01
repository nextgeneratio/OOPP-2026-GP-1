package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.LearningActivityController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionDraft;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionView;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuizView;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.AssignmentRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LearningActivitiesScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final LearningActivityController activities;
    private final CourseController courses;

    public LearningActivitiesScreen(AuthenticatedSession actor, LearningActivityController activities, CourseController courses) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.activities = activities;
        this.courses = courses;
        setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(new JLabel("<html><h2>Quizzes and assignments</h2>Quizzes permit one attempt per active "
                + "enrolment; quiz passwords are stored as hashes. Assignment uploads are limited to 10 MB.</html>"),
                BorderLayout.NORTH);
        JPanel actions = new JPanel(new GridLayout(0, 2, 10, 10));
        if (actor.role() == Role.LECTURER || actor.role() == Role.TECHNICAL_OFFICER) {
            button(actions, "Create MCQ quiz", this::createQuiz);
            button(actions, "Publish / hide quiz", this::toggleQuiz);
            button(actions, "List quizzes", this::listQuizzes);
            button(actions, "Create assignment", this::createAssignment);
            button(actions, "Publish / hide assignment", this::toggleAssignment);
            button(actions, "List assignments", this::listAssignments);
        } else {
            button(actions, "Attempt quiz", this::attemptQuiz);
            button(actions, "View assignments", this::studentAssignments);
            button(actions, "Submit assignment file", this::submitAssignment);
        }
        add(actions, BorderLayout.CENTER);
    }

    private void createQuiz() {
        JComboBox<SelectionOption> offering = offerings();
        JTextField title = field();
        JPasswordField password = new JPasswordField();
        JTextField opens = field(LocalDateTime.now().withSecond(0).withNano(0).toString());
        JTextField closes = field(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString());
        JTextArea questionText = new JTextArea(8, 52);
        questionText.setText("Question|Choice A|Choice B|Choice C|Choice D|correct option number 1-4");
        JPanel panel = form("Course offering", offering, "Quiz title", title, "Quiz password (8+ characters)",
                password, "Opens at yyyy-MM-ddTHH:mm", opens, "Closes at yyyy-MM-ddTHH:mm", closes);
        panel.add(new JLabel("One MCQ per line: prompt|A|B|C|D|correct choice 1-4"));
        panel.add(new javax.swing.JScrollPane(questionText));
        JScrollPane scroll = new JScrollPane(panel); scroll.setBorder(null);
        scroll.setPreferredSize(new java.awt.Dimension(680, 500));
        if (JOptionPane.showConfirmDialog(this, scroll, "Create MCQ quiz",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] secret = password.getPassword();
        try {
            List<QuestionDraft> questions = parseQuestions(questionText.getText());
            show(activities.createQuiz(actor, new LearningActivityController.CreateQuizRequest(
                selectedId(offering), title.getText(), secret, LocalDateTime.parse(opens.getText().trim()),
                    LocalDateTime.parse(closes.getText().trim()), questions)));
        } finally {
            Arrays.fill(secret, '\0');
            password.setText("");
        }
    }

    private List<QuestionDraft> parseQuestions(String raw) {
        List<QuestionDraft> questions = new ArrayList<>();
        for (String line : raw.lines().map(String::trim).filter(line -> !line.isEmpty()).toList()) {
            String[] parts = line.split("\\|", -1);
            if (parts.length != 6) {
                throw new IllegalArgumentException("Each question line must contain exactly six | separated fields.");
            }
            int correct = Integer.parseInt(parts[5].trim()) - 1;
            questions.add(new QuestionDraft(parts[0].trim(),
                    List.of(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim()), correct));
        }
        return List.copyOf(questions);
    }

    private void toggleQuiz() {
        JComboBox<SelectionOption> offering = offerings();
        JComboBox<SelectionOption> quiz = new JComboBox<>();
        refreshQuizOptions(offering, quiz);
        offering.addActionListener(event -> refreshQuizOptions(offering, quiz));
        JTextField visible = field("true or false");
        if (!confirmForm("Publish / hide quiz", new String[]{"Course offering", "Quiz", "Visible true/false"},
                offering, quiz, visible)) return;
        show(activities.setQuizVisibility(actor, new LearningActivityController.VisibilityRequest(
                selectedId(offering), selectedId(quiz), parseBoolean(visible))));
    }

    private void listQuizzes() {
        JComboBox<SelectionOption> offering = offerings();
        if (!confirmForm("List quizzes", new String[]{"Course offering"}, offering)) return;
        ApiResponse<List<QuizView>> response = activities.quizzesForLecturer(actor, selectedId(offering));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String listing = response.data().stream().map(q -> String.format(
                "%s | %s — %s", q.title(), q.opensAt(), q.closesAt()))
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, listing.isBlank() ? "No quizzes are available." : listing);
    }

    private void createAssignment() {
        JComboBox<SelectionOption> offering = offerings();
        JTextField title = field();
        JTextArea description = new JTextArea(5, 45);
        JTextField dueAt = field(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString());
        JTextField visible = field("true or false");
        JPanel panel = form("Course offering", offering, "Title", title,
                "Due yyyy-MM-ddTHH:mm", dueAt, "Visible true/false", visible);
        panel.add(new JLabel("Description"));
        panel.add(new javax.swing.JScrollPane(description));
        JScrollPane scroll = new JScrollPane(panel); scroll.setBorder(null);
        scroll.setPreferredSize(new java.awt.Dimension(620, 420));
        if (JOptionPane.showConfirmDialog(this, scroll, "Create assignment",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        show(activities.createAssignment(actor, new LearningActivityController.CreateAssignmentRequest(
                selectedId(offering), title.getText(), description.getText(),
                LocalDateTime.parse(dueAt.getText().trim()), parseBoolean(visible))));
    }

    private void toggleAssignment() {
        JComboBox<SelectionOption> offering = offerings();
        JComboBox<SelectionOption> assignment = new JComboBox<>();
        refreshAssignmentOptions(offering, assignment);
        offering.addActionListener(event -> refreshAssignmentOptions(offering, assignment));
        JTextField visible = field("true or false");
        if (!confirmForm("Publish / hide assignment", new String[]{"Course offering", "Assignment", "Visible"},
                offering, assignment, visible)) return;
        show(activities.setAssignmentVisibility(actor, new LearningActivityController.VisibilityRequest(
                selectedId(offering), selectedId(assignment), parseBoolean(visible))));
    }

    private void listAssignments() {
        JComboBox<SelectionOption> offering = offerings();
        if (!confirmForm("List assignments", new String[]{"Course offering"}, offering)) return;
        ApiResponse<List<AssignmentRow>> response = activities.assignmentsForLecturer(actor, selectedId(offering));
        showAssignments(response);
    }

    private void attemptQuiz() {
        JComboBox<SelectionOption> offering = offerings();
        if (!confirmForm("Select enrolled offering", new String[]{"Course offering"}, offering)) return;
        ApiResponse<List<QuizView>> quizzes = activities.quizzesForStudent(actor, selectedId(offering));
        if (!quizzes.successful()) {
            error(quizzes.error().message());
            return;
        }
        JComboBox<SelectionOption> quiz = options(quizzes.data().stream().map(q -> new SelectionOption(q.id(),q.title())).toList());
        JPasswordField password = new JPasswordField();
        if (!confirmForm("Available quizzes", new String[]{"Quiz", "Quiz password"}, quiz, password)) return;
        int id = selectedId(quiz);
        char[] secret = password.getPassword();
        try {
            ApiResponse<List<QuestionView>> questionResponse = activities.questionsForStudent(actor,
                    new LearningActivityController.QuestionRequest(id, secret));
            if (!questionResponse.successful()) {
                error(questionResponse.error().message());
                return;
            }
            Map<Integer, Integer> answers = new HashMap<>();
            for (QuestionView question : questionResponse.data()) {
                Object selected = JOptionPane.showInputDialog(this, question.prompt(),
                        "Question", JOptionPane.QUESTION_MESSAGE, null,
                        question.options().toArray(), question.options().get(0));
                if (selected == null) return;
                answers.put(question.id(), question.options().indexOf(selected));
            }
            ApiResponse<lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuizAttemptView> result =
                    activities.attemptQuiz(actor,
                            new LearningActivityController.AttemptRequest(id, secret, answers));
            if (result.successful()) {
                JOptionPane.showMessageDialog(this, "Score: " + result.data().score()
                        + " / " + result.data().total());
            } else {
                error(result.error().message());
            }
        } finally {
            Arrays.fill(secret, '\0');
            password.setText("");
        }
    }

    private void studentAssignments() {
        JComboBox<SelectionOption> offering = offerings();
        if (!confirmForm("Assignments", new String[]{"Enrolled offering"}, offering)) return;
        showAssignments(activities.assignmentsForStudent(actor, selectedId(offering)));
    }

    private void submitAssignment() {
        JComboBox<SelectionOption> offering = offerings();
        if (!confirmForm("Select enrolled offering", new String[]{"Course offering"}, offering)) return;
        ApiResponse<List<AssignmentRow>> rows = activities.assignmentsForStudent(actor, selectedId(offering));
        if (!rows.successful()) { error(rows.error().message()); return; }
        JComboBox<SelectionOption> assignment=options(rows.data().stream().map(a->new SelectionOption(a.id(),a.title()+" — due "+a.dueAt())).toList());
        if (!confirmForm("Submit assignment", new String[]{"Assignment"}, assignment)) return;
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        show(activities.submitAssignment(actor, new LearningActivityController.SubmitRequest(
                selectedId(assignment), Path.of(chooser.getSelectedFile().getAbsolutePath()))));
    }

    private void showAssignments(ApiResponse<List<AssignmentRow>> response) {
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String listing = response.data().stream().map(a -> String.format(
                "%s | due %s | %s%n%s", a.title(), a.dueAt(),
                a.visible() ? "published" : "hidden", a.description()))
                .collect(java.util.stream.Collectors.joining("\n\n"));
        JOptionPane.showMessageDialog(this, listing.isBlank() ? "No assignments are available." : listing);
    }

    private JPanel form(Object... labelFieldPairs) {
        JPanel panel = new JPanel(new GridLayout(labelFieldPairs.length / 2, 2, 8, 8));
        for (int i = 0; i < labelFieldPairs.length; i += 2) {
            panel.add(new JLabel((String) labelFieldPairs[i]));
            panel.add((java.awt.Component) labelFieldPairs[i + 1]);
        }
        return panel;
    }

    private boolean confirmForm(String title, String[] labels, java.awt.Component... fields) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(fields[i]);
        }
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.setPreferredSize(new java.awt.Dimension(650, Math.min(500, labels.length * 52 + 30)));
        return JOptionPane.showConfirmDialog(this, scroll, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private JTextField field() {
        return new JTextField(20);
    }

    private JTextField field(String text) {
        return new JTextField(text, 28);
    }

    private JComboBox<SelectionOption> offerings() {
        ApiResponse<List<OfferingView>> response = courses.offeringViewsForActor(actor);
        if (!response.successful()) { error(response.error().message()); return options(List.of()); }
        return options(response.data().stream().map(view -> new SelectionOption(view.offering().id(),
                view.course().code() + " — " + view.course().title() + " — " + view.semesterLabel())).toList());
    }

    private JComboBox<SelectionOption> options(List<SelectionOption> rows) {
        JComboBox<SelectionOption> combo = new JComboBox<>();
        rows.forEach(combo::addItem);
        if (rows.isEmpty()) combo.addItem(new SelectionOption(0, "No matching records"));
        SelectionOption.enableSearch(combo);
        return combo;
    }

    private int selectedId(JComboBox<SelectionOption> combo) {
        Object selected = combo.getSelectedItem();
        if (selected instanceof SelectionOption option && option.id() > 0) return option.id();
        throw new IllegalArgumentException("No authorized item is available for this selection.");
    }

    private void refreshQuizOptions(JComboBox<SelectionOption> offering, JComboBox<SelectionOption> quiz) {
        int offeringId = selectedId(offering);
        ApiResponse<List<QuizView>> response = activities.quizzesForLecturer(actor, offeringId);
        if (!response.successful()) { error(response.error().message()); optionsInto(quiz, List.of()); return; }
        optionsInto(quiz, response.data().stream().map(q -> new SelectionOption(q.id(), q.title())).toList());
    }

    private void refreshAssignmentOptions(JComboBox<SelectionOption> offering, JComboBox<SelectionOption> assignment) {
        int offeringId = selectedId(offering);
        ApiResponse<List<AssignmentRow>> response = activities.assignmentsForLecturer(actor, offeringId);
        if (!response.successful()) { error(response.error().message()); optionsInto(assignment, List.of()); return; }
        optionsInto(assignment, response.data().stream().map(a -> new SelectionOption(a.id(), a.title()+" — due "+a.dueAt())).toList());
    }

    private void optionsInto(JComboBox<SelectionOption> combo, List<SelectionOption> rows) {
        combo.removeAllItems(); rows.forEach(combo::addItem);
        if (rows.isEmpty()) combo.addItem(new SelectionOption(0, "No matching records"));
    }

    private boolean parseBoolean(JTextField field) {
        String value = field.getText().trim();
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException("Enter true or false for visibility.");
        }
        return Boolean.parseBoolean(value);
    }

    private void button(JPanel panel, String text, Runnable action) {
        javax.swing.JButton button = new javax.swing.JButton(text);
        button.addActionListener(event -> {
            try {
                action.run();
            } catch (IllegalArgumentException | java.time.DateTimeException invalid) {
                error("Check the selected course, question syntax, date, or visibility value: " + invalid.getMessage());
            }
        });
        panel.add(button);
    }

    private void show(ApiResponse<?> response) {
        if (response.successful()) {
            JOptionPane.showMessageDialog(this, "Action completed successfully.");
        } else {
            error(response.error().message());
        }
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Learning activities", JOptionPane.ERROR_MESSAGE);
    }
}
