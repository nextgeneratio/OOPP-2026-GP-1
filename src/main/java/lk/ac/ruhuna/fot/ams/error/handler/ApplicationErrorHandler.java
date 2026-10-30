package lk.ac.ruhuna.fot.ams.error.handler;

import lk.ac.ruhuna.fot.ams.error.exception.ApplicationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApplicationErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationErrorHandler.class);

    public String errorCode(Throwable failure) {
        if (failure instanceof AuthenticationException) {
            return "AUTHENTICATION_FAILED";
        }
        if (failure instanceof AuthorizationException) {
            return "FORBIDDEN";
        }
        if (failure instanceof ValidationException) {
            return "VALIDATION_ERROR";
        }
        if (failure instanceof BusinessRuleException) {
            return "BUSINESS_RULE_VIOLATION";
        }
        if (failure instanceof NotFoundException) {
            return "NOT_FOUND";
        }
        if (failure instanceof DataAccessException) {
            return "DATA_ACCESS_ERROR";
        }
        return "INTERNAL_ERROR";
    }

    public String userMessage(Throwable failure) {
        if (failure instanceof AuthenticationException) {
            return "Username or password is incorrect.";
        }
        if (failure instanceof AuthorizationException) {
            return "You do not have permission for this action.";
        }
        if (failure instanceof ValidationException) {
            return failure.getMessage();
        }
        if (failure instanceof BusinessRuleException) {
            return failure.getMessage();
        }
        if (failure instanceof NotFoundException) {
            return "The requested record is unavailable.";
        }
        if (failure instanceof DataAccessException) {
            java.sql.SQLException sqlFailure = findSqlFailure(failure);
            if (sqlFailure != null && ("42S22".equals(sqlFailure.getSQLState())
                    || sqlFailure.getErrorCode() == 1054)) {
                return "The database schema is missing a column required by this operation. Apply the matching compatibility SQL, then restart the application.";
            }
            if (sqlFailure != null && ("42S02".equals(sqlFailure.getSQLState())
                    || sqlFailure.getErrorCode() == 1146)) {
                return "The database schema is missing a table required by this operation. Apply the matching compatibility SQL, then restart the application.";
            }
            if (sqlFailure != null) {
                String state = sqlFailure.getSQLState() == null ? "unknown" : sqlFailure.getSQLState();
                return "The database operation failed (SQL state " + state + ", vendor code "
                        + sqlFailure.getErrorCode() + "). Check the database schema, server availability, account "
                        + "permissions, and data constraints; inspect the application log for operation context.";
            }
            return "The database operation failed. Check the configured database, server availability, schema, and account permissions; then inspect the application log for the SQL state and vendor code.";
        }
        return "The operation could not be completed.";
    }

    public void log(Throwable failure) {
        if (failure instanceof DataAccessException) {
            java.sql.SQLException sqlFailure = findSqlFailure(failure);
            if (sqlFailure != null) {
                LOGGER.warn("Database operation failed (cause={}, SQLState={}, vendorCode={}).",
                        sqlFailure.getClass().getSimpleName(), sqlFailure.getSQLState(), sqlFailure.getErrorCode());
            } else {
                LOGGER.warn("Database operation failed (cause={}).",
                        failure.getCause() == null ? "unknown" : failure.getCause().getClass().getSimpleName());
            }
        } else if (failure instanceof ApplicationException) {
            LOGGER.warn("Application operation failed: {}", failure.getMessage(), failure);
        } else {
            LOGGER.error("Unexpected application failure", failure);
        }
    }

    private java.sql.SQLException findSqlFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof java.sql.SQLException sqlFailure) return sqlFailure;
            current = current.getCause();
        }
        return null;
    }
}
