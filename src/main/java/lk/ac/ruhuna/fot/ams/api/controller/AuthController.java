package lk.ac.ruhuna.fot.ams.api.controller;

import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public AuthenticatedSession login(String username, char[] password) {
        return authService.authenticate(username, password);
    }
}
