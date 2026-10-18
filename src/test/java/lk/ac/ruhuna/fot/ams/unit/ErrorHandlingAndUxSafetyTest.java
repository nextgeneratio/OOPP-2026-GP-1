package lk.ac.ruhuna.fot.ams.unit;

import java.sql.SQLException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAndUxSafetyTest {
    private final ApplicationErrorHandler errorHandler = new ApplicationErrorHandler();

    @Test
    void authenticationFailureDoesNotRevealAccountDetails() {
        String message = errorHandler.userMessage(new AuthenticationException("Bad credentials for admin."));

        assertThat(message).isEqualTo("Username or password is incorrect.");
        assertThat(message).doesNotContain("admin");
    }

    @Test
    void authorizationFailureDoesNotRevealResourceDetails() {
        String message = errorHandler.userMessage(new AuthorizationException("Denied access to grade table."));

        assertThat(message).isEqualTo("You do not have permission for this action.");
        assertThat(message).doesNotContain("grade table");
    }

    @Test
    void validationFailurePreservesActionableMessage() {
        String message = errorHandler.userMessage(new ValidationException("Score must be between 0 and 100."));

        assertThat(message).isEqualTo("Score must be between 0 and 100.");
    }

    @Test
    void missingRecordDoesNotRevealInternalIdentifiers() {
        String message = errorHandler.userMessage(new NotFoundException("Record 123 was not found."));

        assertThat(message).isEqualTo("The requested record is unavailable.");
        assertThat(message).doesNotContain("123");
    }

    @Test
    void databaseFailureDoesNotRevealSqlDetails() {
        DataAccessException failure = new DataAccessException(
                "Query failed", new SQLException("SELECT * FROM users"));

        String message = errorHandler.userMessage(failure);

        assertThat(message).isEqualTo("The database is currently unavailable. Please try again.");
        assertThat(message).doesNotContain("SELECT", "users");
    }

    @Test
    void unexpectedFailureUsesSafeGenericMessage() {
        String message = errorHandler.userMessage(new IllegalStateException("internal detail"));

        assertThat(message).isEqualTo("The operation could not be completed.");
        assertThat(message).doesNotContain("internal detail");
    }
}
