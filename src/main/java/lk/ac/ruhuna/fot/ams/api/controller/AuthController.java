package lk.ac.ruhuna.fot.ams.api.controller;

import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

import java.util.Arrays;
import java.util.Objects;

public final class AuthController {
    public static final class LoginRequest {
        private final String username;
        private final char[] password;

        public LoginRequest(String username, char[] password) {
            this.username = username;
            this.password = password == null ? null : password.clone();
        }

        public String username() {
            return username;
        }

        public char[] takePassword() {
            if (password == null) {
                return null;
            }
            char[] copy = password.clone();
            Arrays.fill(password, '\0');
            return copy;
        }

        @Override
        public String toString() {
            return "LoginRequest[username=" + username + ", password=<redacted>]";
        }
    }

    public record LoginResponse(AuthenticatedSession session) {
    }

    private final AuthService authService;
    private final ApiControllerSupport support;

    public AuthController(AuthService authService, ApiControllerSupport support) {
        this.authService = Objects.requireNonNull(authService);
        this.support = Objects.requireNonNull(support);
    }

    public AuthController(AuthService authService) {
        this(authService, new ApiControllerSupport(new lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler()));
    }

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        return support.execute(() -> {
            if (request == null) {
                throw new lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException(
                        "Username or password is incorrect.");
            }
            char[] password = request.takePassword();
            try {
                return new LoginResponse(authService.authenticate(request.username(), password));
            } finally {
                if (password != null) {
                    Arrays.fill(password, '\0');
                }
            }
        });
    }

    public AuthenticatedSession login(String username, char[] password) {
        return authService.authenticate(username, password);
    }
}
