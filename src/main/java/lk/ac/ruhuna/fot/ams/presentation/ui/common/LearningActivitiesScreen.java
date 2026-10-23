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
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.LearningActivityController;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionDraft;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionView;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuizView;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.AssignmentRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LearningActivitiesScreen extends JPanel {
    private final AuthenticatedSession actor;
    private final LearningActivityController activities;

    public LearningActivitiesScreen(AuthenticatedSession actor, LearningActivityController activities) {
        super(new BorderLayout(12, 12));
        this.actor = actor;
        this.activities = activities;
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
        JTextField offering = field();
        JTextField title = field();
        JPasswordField password = new JPasswordField();
        JTextField opens = field(LocalDateTime.now().withSecond(0).withNano(0).toString());
        JTextField closes = field(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString());
        JTextArea questionText = new JTextArea(8, 52);
        questionText.setText("Question|Choice A|Choice B|Choice C|Choice D|correct option number 1-4");
        JPanel panel = form("Offering ID", offering, "Quiz title", title, "Quiz password (8+ characters)",
                password, "Opens at yyyy-MM-ddTHH:mm", opens, "Closes at yyyy-MM-ddTHH:mm", closes);
        panel.add(new JLabel("One MCQ per line: prompt|A|B|C|D|correct choice 1-4"));
        panel.add(new javax.swing.JScrollPane(questionText));
        if (JOptionPane.showConfirmDialog(this, panel, "Create MCQ quiz",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] secret = password.getPassword();
        try {
            List<QuestionDraft> questions = parseQuestions(questionText.getText());
            show(activities.createQuiz(actor, new LearningActivityController.CreateQuizRequest(
                    integer(offering), title.getText(), secret, LocalDateTime.parse(opens.getText().trim()),
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
        JTextField offering = field();
        JTextField quiz = field();
        JTextField visible = field("true or false");
        if (!confirmForm("Publish / hide quiz", new String[]{"Offering ID", "Quiz ID", "Visible true/false"},
                offering, quiz, visible)) return;
        show(activities.setQuizVisibility(actor, new LearningActivityController.VisibilityRequest(
                integer(offering), integer(quiz), parseBoolean(visible))));
    }

    private void listQuizzes() {
        JTextField offering = field();
        if (!confirmForm("List quizzes", new String[]{"Offering ID"}, offering)) return;
        ApiResponse<List<QuizView>> response = activities.quizzesForLecturer(actor, integer(offering));
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String listing = response.data().stream().map(q -> String.format(
                "%d | %s | %s — %s", q.id(), q.title(), q.opensAt(), q.closesAt()))
                .collect(java.util.stream.Collectors.joining("\n"));
        JOptionPane.showMessageDialog(this, listing.isBlank() ? "No quizzes are available." : listing);
    }

    private void createAssignment() {
        JTextField offering = field();
        JTextField title = field();
        JTextArea description = new JTextArea(5, 45);
        JTextField dueAt = field(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0).toString());
        JTextField visible = field("true or false");
        JPanel panel = form("Offering ID", offering, "Title", title,
                "Due yyyy-MM-ddTHH:mm", dueAt, "Visible true/false", visible);
        panel.add(new JLabel("Description"));
        panel.add(new javax.swing.JScrollPane(description));
        if (JOptionPane.showConfirmDialog(this, panel, "Create assignment",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        show(activities.createAssignment(actor, new LearningActivityController.CreateAssignmentRequest(
                integer(offering), title.getText(), description.getText(),
                LocalDateTime.parse(dueAt.getText().trim()), parseBoolean(visible))));
    }

    private void toggleAssignment() {
        JTextField offering = field();
        JTextField assignment = field();
        JTextField visible = field("true or false");
        if (!confirmForm("Publish / hide assignment", new String[]{"Offering ID", "Assignment ID", "Visible"},
                offering, assignment, visible)) return;
        show(activities.setAssignmentVisibility(actor, new LearningActivityController.VisibilityRequest(
                integer(offering), integer(assignment), parseBoolean(visible))));
    }

    private void listAssignments() {
        JTextField offering = field();
        if (!confirmForm("List assignments", new String[]{"Offering ID"}, offering)) return;
        ApiResponse<List<AssignmentRow>> response = activities.assignmentsForLecturer(actor, integer(offering));
        showAssignments(response);
    }

    private void attemptQuiz() {
        JTextField offering = field();
        if (!confirmForm("Select enrolled offering", new String[]{"Offering ID"}, offering)) return;
        ApiResponse<List<QuizView>> quizzes = activities.quizzesForStudent(actor, integer(offering));
        if (!quizzes.successful()) {
            error(quizzes.error().message());
            return;
        }
        String list = quizzes.data().stream().map(q -> q.id() + " | " + q.title())
                .collect(java.util.stream.Collectors.joining("\n"));
        JTextField quizId = field();
        JPasswordField password = new JPasswordField();
        if (!confirmForm("Available quizzes\n" + (list.isBlank() ? "No quizzes available." : list),
                new String[]{"Quiz ID", "Quiz password"}, quizId, password)) return;
        int id = integer(quizId);
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
        JTextField offering = field();
        if (!confirmForm("Assignments", new String[]{"Enrolled offering ID"}, offering)) return;
        showAssignments(activities.assignmentsForStudent(actor, integer(offering)));
    }

    private void submitAssignment() {
        JTextField assignment = field();
        if (!confirmForm("Submit assignment", new String[]{"Assignment ID"}, assignment)) return;
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        show(activities.submitAssignment(actor, new LearningActivityController.SubmitRequest(
                integer(assignment), Path.of(chooser.getSelectedFile().getAbsolutePath()))));
    }

    private void showAssignments(ApiResponse<List<AssignmentRow>> response) {
        if (!response.successful()) {
            error(response.error().message());
            return;
        }
        String listing = response.data().stream().map(a -> String.format(
                "%d | %s | due %s | %s%n%s", a.id(), a.title(), a.dueAt(),
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
        return JOptionPane.showConfirmDialog(this, panel, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private JTextField field() {
        return new JTextField(20);
    }

    private JTextField field(String text) {
        return new JTextField(text, 28);
    }

    private int integer(JTextField field) {
        return Integer.parseInt(field.getText().trim());
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
                error("Check the IDs, question syntax, date, or visibility value: " + invalid.getMessage());
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
