package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.api.request.LoginRequest;
import lk.ac.ruhuna.fot.ams.api.response.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.response.LoginResponse;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthController {
    private final AuthService authService;
    private final ApplicationErrorHandler errorHandler;

    public AuthController(AuthService authService) {
        this(authService, new ApplicationErrorHandler());
    }

    public AuthController(AuthService authService, ApplicationErrorHandler errorHandler) {
        this.authService = Objects.requireNonNull(authService, "AuthService must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "ApplicationErrorHandler must not be null");
    }

    public AuthenticatedSession login(String username, char[] password) {
        return authService.authenticate(username, password);
    }

    public ApiResponse<LoginResponse> handleLogin(LoginRequest request) {
        if (request == null) {
            return ApiResponse.error("Request payload cannot be null.");
        }
        if (request.username() == null || request.username().isBlank()) {
            return ApiResponse.error("Username is required.");
        }
        if (request.password() == null || request.password().length == 0) {
            return ApiResponse.error("Password is required.");
        }

        try {
            AuthenticatedSession session = authService.authenticate(request.username(), request.password());
            LoginResponse responseData = new LoginResponse(
                    session.userId(),
                    session.username(),
                    session.role(),
                    session.displayName()
            );
            return ApiResponse.success(responseData);
        } catch (Exception failure) {
            errorHandler.log(failure);
            return ApiResponse.error(errorHandler.userMessage(failure));
        }
    }
}
