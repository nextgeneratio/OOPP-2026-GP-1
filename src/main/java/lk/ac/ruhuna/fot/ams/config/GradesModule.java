package lk.ac.ruhuna.fot.ams.config;

import lk.ac.ruhuna.fot.ams.api.controller.ApiControllerSupport;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController;
import lk.ac.ruhuna.fot.ams.business.service.GradesService;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcCourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcCourseResultDao;

/** Composition for the grades/GPA read layer; reuses the already-configured GpaService and authorization. */
public final class GradesModule {
    private GradesModule() {
    }

    public static GradesController create(ApplicationComponents components) {
        JdbcExecutor jdbc = new JdbcExecutor(components.connectionProvider());
        GradesService service = new GradesService(new JdbcCourseResultDao(jdbc), new JdbcCourseDao(jdbc),
                components.gpaService(), components.authorizationService());
        return new GradesController(service, new ApiControllerSupport(components.errorHandler()));
    }
}
