package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.CourseCatalog;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.NoticeRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Notices for every role, plus a compose form for Administrators. Visibility and validity are decided by ContentService. */
public final class NoticesScreen extends JPanel {
    private final AuthenticatedSession session;
    private final ContentController content;
    private final boolean admin;

    private final ObjectTableModel<NoticeRow> model = new ObjectTableModel<>(List.of(
            ObjectTableModel.column("Title", NoticeRow::title),
            ObjectTableModel.column("Audience", this::audienceText),
            ObjectTableModel.column("Published", n -> Formats.dateTime(n.publishDate())),
            ObjectTableModel.column("Expires", n -> n.expiryDate() == null ? "No expiry" : Formats.dateTime(n.expiryDate()))));
    private final JTable table = new JTable(model);
    private final JTextArea detail = new JTextArea();
    private final JLabel status = new JLabel(" ");

    private final JTextField titleField = new JTextField();
    private final JTextArea bodyArea = new JTextArea(5, 20);
    private final JComboBox<String> audienceBox = new JComboBox<>(new String[]{"ALL", "ROLE", "BATCH"});
    private final JComboBox<Role> roleBox = new JComboBox<>(Role.values());
    private final JComboBox<SelectionOption> batchBox = new JComboBox<>();
    private final JTextField publishField = new JTextField();
    private final JTextField expiryField = new JTextField();
    private final FormField titleRow = new FormField("Title", titleField);
    private final FormField roleRow = new FormField("Target role", roleBox);
    private final FormField batchRow = new FormField("Target batch", batchBox);
    private final FormField publishRow = new FormField("Publish date (" + Formats.DATE_TIME_HINT + ")", publishField);
    private final FormField expiryRow = new FormField("Expiry date, optional (" + Formats.DATE_TIME_HINT + ")", expiryField);
    private final JLabel formMessage = new JLabel(" ");
    private CourseCatalog catalog;

    public NoticesScreen(AuthenticatedSession session, ContentController content, CourseController courses) {
        super(new BorderLayout(UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        this.session = session;
        this.content = content;
        if (session.role() == Role.ADMIN) {
            var catalogResponse = courses.catalog(session);
            if (catalogResponse.successful()) {
                catalog = catalogResponse.data();
                catalog.batches().forEach(batch -> batchBox.addItem(new SelectionOption(batch.id(),
                        batch.code() + " — Level " + batch.level() + " — " + batch.intakeYear())));
            }
        }
        SelectionOption.enableSearch(batchBox);
        this.admin = session.role() == Role.ADMIN;
        setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG));
        JLabel title = new JLabel("Notices");
        title.setFont(UiTheme.pageTitle());
        add(title, BorderLayout.NORTH);

        detail.setEditable(false);
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        detail.setFont(UiTheme.body());
        detail.getAccessibleContext().setAccessibleName("Notice detail");
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                show(model.rowAt(table.convertRowIndexToModel(table.getSelectedRow())));
            }
        });
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, ObjectTableModel.styledScroll(table), new JScrollPane(detail));
        split.setResizeWeight(0.55);
        JPanel center = new JPanel(new BorderLayout(0, UiTheme.SPACE_SM));
        center.setOpaque(false);
        center.add(split, BorderLayout.CENTER);
        status.setFont(UiTheme.helper());
        status.setForeground(UiTheme.MUTED);
        center.add(status, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
        if (admin) {
            add(buildComposer(), BorderLayout.EAST);
        }
        load();
    }

    private String audienceText(NoticeRow notice) {
        return switch (notice.audience()) {
            case "ROLE" -> NavigationModule.roleLabel(notice.targetRole()) + " only";
            case "BATCH" -> catalog == null ? "Selected batch" : catalog.batches().stream()
                    .filter(batch -> batch.id() == notice.targetBatchId())
                    .map(batch -> batch.code() + " — Level " + batch.level()).findFirst().orElse("Selected batch");
            default -> "Everyone";
        };
    }

    private void show(NoticeRow notice) {
        detail.setText(notice.title() + "\n" + audienceText(notice) + "  |  Published " + Formats.dateTime(notice.publishDate())
                + "  |  " + (notice.expiryDate() == null ? "No expiry" : "Expires " + Formats.dateTime(notice.expiryDate()))
                + "\n\n" + notice.body());
        detail.setCaretPosition(0);
    }

    private JScrollPane buildComposer() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD, UiTheme.SPACE_MD));
        JLabel heading = new JLabel("Compose notice");
        heading.setFont(UiTheme.sectionTitle());
        heading.setAlignmentX(LEFT_ALIGNMENT);
        Combos.describe(audienceBox, value -> switch (value) {
            case "ROLE" -> "A role";
            case "BATCH" -> "A batch";
            default -> "Everyone";
        });
        Combos.describe(roleBox, NavigationModule::roleLabel);
        audienceBox.addActionListener(event -> syncAudience());
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        publishField.setText(Formats.dateTime(LocalDateTime.now().withSecond(0).withNano(0)));
        formMessage.setFont(UiTheme.helper());
        formMessage.setForeground(UiTheme.ERROR);
        formMessage.setAlignmentX(LEFT_ALIGNMENT);
        formMessage.getAccessibleContext().setAccessibleName("Compose message");
        AppButton publish = new AppButton("Publish notice");
        publish.addActionListener(event -> publish());
        publish.setAlignmentX(LEFT_ALIGNMENT);
        form.add(heading);
        form.add(javax.swing.Box.createVerticalStrut(UiTheme.SPACE_MD));
        form.add(titleRow);
        form.add(new FormField("Body", bodyScroll));
        form.add(new FormField("Audience", audienceBox));
        form.add(roleRow);
        form.add(batchRow);
        form.add(publishRow);
        form.add(expiryRow);
        form.add(formMessage);
        form.add(publish);
        syncAudience();
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setPreferredSize(new Dimension(380, 0));
        return scroll;
    }

    private void syncAudience() {
        String audience = (String) audienceBox.getSelectedItem();
        roleRow.setVisible("ROLE".equals(audience));
        batchRow.setVisible("BATCH".equals(audience));
    }

    private void load() {
        status.setText("Loading\u2026");
        ApiCall.run(() -> content.visibleNotices(session), notices -> {
            model.setRows(notices);
            detail.setText("");
            status.setForeground(UiTheme.MUTED);
            status.setText(notices.isEmpty() ? "There are no current notices." : notices.size() + " current notice(s). Expired notices are not listed.");
        }, error -> {
            status.setForeground(UiTheme.ERROR);
            status.setText("\u26A0 " + error.message());
        });
    }

    private void publish() {
        formMessage.setText(" ");
        publishRow.clearError();
        expiryRow.clearError();
        batchRow.clearError();
        LocalDateTime publishDate;
        LocalDateTime expiryDate = null;
        Integer batchId = null;
        try {
            publishDate = Formats.parseDateTime(publishField.getText());
        } catch (DateTimeParseException invalid) {
            publishRow.setError("Use " + Formats.DATE_TIME_HINT + ".");
            return;
        }
        if (!expiryField.getText().isBlank()) {
            try {
                expiryDate = Formats.parseDateTime(expiryField.getText());
            } catch (DateTimeParseException invalid) {
                expiryRow.setError("Use " + Formats.DATE_TIME_HINT + " or leave blank.");
                return;
            }
        }
        String audience = (String) audienceBox.getSelectedItem();
        if ("BATCH".equals(audience)) {
            Object selected = batchBox.getSelectedItem();
            if (!(selected instanceof SelectionOption option) || option.id() <= 0) {
                batchRow.setError("Choose an available batch.");
                return;
            }
            batchId = option.id();
        }
        Role role = "ROLE".equals(audience) ? (Role) roleBox.getSelectedItem() : null;
        NoticeRow notice = new NoticeRow(0, titleField.getText().trim(), bodyArea.getText().trim(), audience,
                role, batchId, publishDate, expiryDate);
        ApiCall.run(() -> content.createNotice(session, notice), created -> {
            titleField.setText("");
            bodyArea.setText("");
            expiryField.setText("");
            formMessage.setForeground(UiTheme.SUCCESS);
            formMessage.setText("Notice published.");
            load();
        }, this::failed);
    }

    private void failed(ApiError error) {
        if (Dialogs.isInline(error)) {
            formMessage.setForeground(UiTheme.ERROR);
            formMessage.setText("\u26A0 " + error.message());
        } else {
            Dialogs.showError(this, error);
        }
    }
}
