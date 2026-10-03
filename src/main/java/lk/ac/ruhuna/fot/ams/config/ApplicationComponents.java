package lk.ac.ruhuna.fot.ams.config;

import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcUserDao;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;

public final class ApplicationComponents {
    private final ConnectionProvider connectionProvider;
    private final AuthController authController;
    private final ApplicationErrorHandler errorHandler;
    private final AuthorizationService authorizationService;

    private ApplicationComponents(ConnectionProvider connectionProvider,
                                 AuthController authController,
                                 ApplicationErrorHandler errorHandler,
                                 AuthorizationService authorizationService) {
        this.connectionProvider = connectionProvider;
        this.authController = authController;
        this.errorHandler = errorHandler;
        this.authorizationService = authorizationService;
    }

    public static ApplicationComponents create(AppConfig config) {
        ConnectionProvider connectionProvider = new JdbcConnectionProvider(config);
        PasswordHasher passwordHasher = new PasswordHasher();
        AuthService authService = new AuthService(new JdbcUserDao(connectionProvider), passwordHasher);
        AuthorizationService authorizationService = new AuthorizationService();
        return new ApplicationComponents(connectionProvider, new AuthController(authService),
            new ApplicationErrorHandler(), authorizationService);
    }

    public ConnectionProvider connectionProvider() { return connectionProvider; }
    public AuthController authController() { return authController; }
    public ApplicationErrorHandler errorHandler() { return errorHandler; }
    public AuthorizationService authorizationService() { return authorizationService; }
}
