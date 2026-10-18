package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.JLabel;
import javax.swing.JTable;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UserView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.admin.UserManagementScreen;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

/** Isolated screen test: the controller is mocked, so only the Swing rendering behaviour is checked. */
class UserManagementScreenTest {
    private static final AuthenticatedSession ADMIN = new AuthenticatedSession(1L, "ADM001", Role.ADMIN, "Admin");

    @Test
    void rendersUsersReturnedByTheController() throws Exception {
        UserController controller = mock(UserController.class);
        UserView user = new UserView(5L, "STU001", Role.UNDERGRADUATE, "s@example.lk", true,
                "Student One", null, null, 1, "S001");
        when(controller.search(eq(ADMIN), any())).thenReturn(ApiResponse.success(List.of(user)));

        UserManagementScreen screen = new UserManagementScreen(ADMIN, controller);
        JTable table = first(screen, JTable.class);

        waitUntil(() -> table.getRowCount() == 1);
        assertThat(table.getValueAt(0, 0)).isEqualTo("STU001");
        assertThat(table.getValueAt(0, 4)).isEqualTo("ACTIVE");
    }

    @Test
    void showsAnEmptyStateMessage() throws Exception {
        UserController controller = mock(UserController.class);
        when(controller.search(eq(ADMIN), any())).thenReturn(ApiResponse.success(List.of()));

        UserManagementScreen screen = new UserManagementScreen(ADMIN, controller);

        waitUntil(() -> labelTexts(screen).contains("No users match the current filters."));
    }

    @Test
    void showsASafeMessageWhenTheDatabaseIsUnavailable() throws Exception {
        UserController controller = mock(UserController.class);
        when(controller.search(eq(ADMIN), any())).thenReturn(ApiResponse.failure(
                new ApiError("DATA_ACCESS_ERROR", "The database is currently unavailable. Please try again.")));

        UserManagementScreen screen = new UserManagementScreen(ADMIN, controller);

        waitUntil(() -> labelTexts(screen).stream().anyMatch(text -> text.contains("database is currently unavailable")));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        for (int attempt = 0; attempt < 50 && !condition.getAsBoolean(); attempt++) {
            Thread.sleep(100);
        }
        assertThat(condition.getAsBoolean()).as("the screen reached the expected state").isTrue();
    }

    private static List<String> labelTexts(Container root) {
        List<String> texts = new ArrayList<>();
        collect(root, JLabel.class).forEach(label -> texts.add(label.getText()));
        return texts;
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        return collect(root, type).get(0);
    }

    private static <T extends Component> List<T> collect(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                found.add(type.cast(child));
            }
            if (child instanceof Container container) {
                found.addAll(collect(container, type));
            }
        }
        return found;
    }
}
