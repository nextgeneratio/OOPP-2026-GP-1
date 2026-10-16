package lk.ac.ruhuna.fot.ams.unit;

import lk.ac.ruhuna.fot.ams.error.exception.ApplicationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlingAndUxSafetyTest {

    private final ApplicationErrorHandler errorHandler = new ApplicationErrorHandler();

    @Test
    @DisplayName("AuthenticationException returns clean message without leaking account internals")
    void authenticationException_safeMessage() {
        AuthenticationException ex = new AuthenticationException("Bad credentials for user admin in DB table");
        String message = errorHandler.userMessage(ex);

        assertThat(message).isEqualTo("Username or password is incorrect.");
        assertThat(message).doesNotContain("admin", "DB", "table");
    }

    @Test
    @DisplayName("AuthorizationException returns permission denied message")
    void authorizationException_safeMessage() {
        AuthorizationException ex = new AuthorizationException("User 123 attempted unauthorized write on table grades");
        String message = errorHandler.userMessage(ex);

        assertThat(message).isEqualTo("You do not have permission for this action.");
        assertThat(message).doesNotContain("table", "123", "grades");
    }

    @Test
    @DisplayName("ValidationException returns domain validation message")
    void validationException_safeMessage() {
        ValidationException ex = new ValidationException("Score must be between 0 and 100.");
        String message = errorHandler.userMessage(ex);

        assertThat(message).isEqualTo("Score must be between 0 and 100.");
    }

    @Test
    @DisplayName("NotFoundException returns generic not found message")
    void notFoundException_safeMessage() {
        NotFoundException ex = new NotFoundException("Record with ID 99999 was not found in schema oop_sms");
        String message = errorHandler.userMessage(ex);

        assertThat(message).isEqualTo("The requested record is unavailable.");
        assertThat(message).doesNotContain("99999", "schema", "oop_sms");
    }

    @Test
    @DisplayName("DataAccessException and SQL exceptions mask technical database details")
    void dataAccessException_safeMessage() {
        SQLException sqlEx = new SQLException("Syntax error in SQL statement 'SELECT * FROM users WHERE...'; Table 'users' doesn't exist");
        DataAccessException ex = new DataAccessException("Failed query", sqlEx);

        String message = errorHandler.userMessage(ex);

        assertThat(message).isEqualTo("The database is currently unavailable. Please try again.");
        assertThat(message).doesNotContain("Syntax error", "SELECT", "users");
    }

    @Test
    @DisplayName("Unexpected RuntimeExceptions return generic fallback message")
    void unexpectedException_safeMessage() {
        NullPointerException npe = new NullPointerException("Null reference at line 145 in ServiceImpl.java");
        String message = errorHandler.userMessage(npe);

        assertThat(message).isEqualTo("The operation could not be completed.");
        assertThat(message).doesNotContain("NullPointerException", "line 145", "ServiceImpl.java");
    }

    @Test
    @DisplayName("Logging does not throw errors")
    void logHandling_safeExecution() {
        errorHandler.log(new ValidationException("Validation issue"));
        errorHandler.log(new RuntimeException("General failure"));
    }
}
