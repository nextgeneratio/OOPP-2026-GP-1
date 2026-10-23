package lk.ac.ruhuna.fot.ams.business.service;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Read-only, scope-checked lookups that the Medical screen needs. */
public final class ScreenLookupService {
    private final CourseDao courseDao;
    private final AuthorizationService authorization;

    public ScreenLookupService(CourseDao courseDao, AuthorizationService authorization) {
        this.courseDao = Objects.requireNonNull(courseDao);
        this.authorization = Objects.requireNonNull(authorization);
    }

    /** Active students of an offering the officer is responsible for, including the ids Medical needs. */
    public List<RosterRow> medicalStudents(AuthenticatedSession actor, int offeringId) {
        authorization.requireRole(actor, Role.TECHNICAL_OFFICER);
        authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        return courseDao.findRoster(offeringId);
    }
}
