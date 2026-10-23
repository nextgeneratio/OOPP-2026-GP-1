package lk.ac.ruhuna.fot.ams.config;

import lk.ac.ruhuna.fot.ams.api.controller.ApiControllerSupport;
import lk.ac.ruhuna.fot.ams.api.controller.ScreenLookupController;
import lk.ac.ruhuna.fot.ams.business.service.ScreenLookupService;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcCourseDao;

/** Composition for the Medical screen's student lookup; leaves ApplicationComponents untouched. */
public final class ScreenLookupModule {
    private ScreenLookupModule() {
    }

    public static ScreenLookupController create(ApplicationComponents components) {
        JdbcExecutor jdbc = new JdbcExecutor(components.connectionProvider());
        return new ScreenLookupController(new ScreenLookupService(new JdbcCourseDao(jdbc),
                components.authorizationService()), new ApiControllerSupport(components.errorHandler()));
    }
}
