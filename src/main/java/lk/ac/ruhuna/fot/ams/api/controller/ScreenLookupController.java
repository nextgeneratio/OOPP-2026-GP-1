package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.ScreenLookupService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ScreenLookupController {
    private final ScreenLookupService service;
    private final ApiControllerSupport support;

    public ScreenLookupController(ScreenLookupService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<List<RosterRow>> medicalStudents(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.medicalStudents(actor, offeringId));
    }
}
