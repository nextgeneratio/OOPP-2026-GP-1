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
            return "The database is currently unavailable. Please try again.";
        }
        return "The operation could not be completed.";
    }

    public void log(Throwable failure) {
        if (failure instanceof ApplicationException) {
            LOGGER.warn("Application operation failed: {}", failure.getMessage(), failure);
        } else {
            LOGGER.error("Unexpected application failure", failure);
        }
    }
}
