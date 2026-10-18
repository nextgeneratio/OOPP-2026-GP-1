package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import java.util.function.Supplier;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class ApiControllerSupport {
    private final ApplicationErrorHandler errorHandler;

    public ApiControllerSupport(ApplicationErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler);
    }

    public <T> ApiResponse<T> execute(Supplier<T> operation) {
        try {
            return ApiResponse.success(operation.get());
        } catch (RuntimeException failure) {
            errorHandler.log(failure);
            return ApiResponse.failure(new ApiError(
                    errorHandler.errorCode(failure), errorHandler.userMessage(failure)));
        }
    }

    public <T> ApiResponse<T> execute(Object request, Supplier<T> operation) {
        return execute(() -> {
            if (request == null) {
                throw new ValidationException("A request is required.");
            }
            return operation.get();
        });
    }
}
