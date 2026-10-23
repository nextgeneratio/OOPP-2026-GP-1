package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import lk.ac.ruhuna.fot.ams.business.service.ScreenLookupService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class ScreenLookupServiceTest {
    private final CourseDao courseDao = mock(CourseDao.class);
    private final AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
    private final ScreenLookupService service = new ScreenLookupService(courseDao, new AuthorizationService(scope));
    private final AuthenticatedSession officer = new AuthenticatedSession(2L, "TO001", Role.TECHNICAL_OFFICER, "Officer");
    private final AuthenticatedSession student = new AuthenticatedSession(3L, "STU001", Role.UNDERGRADUATE, "Student");

    @Test
    void officerGetsTheRosterOfAnOfferingTheyAreResponsibleFor() {
        RosterRow row = new RosterRow(5, 7, "STU007", "Student Seven");
        when(scope.technicalOfficerAuthorizedForOffering(anyLong(), eq(1))).thenReturn(true);
        when(courseDao.findRoster(1)).thenReturn(List.of(row));

        assertThat(service.medicalStudents(officer, 1)).containsExactly(row);
    }

    @Test
    void officerCannotReadAnotherDepartmentsOffering() {
        when(scope.technicalOfficerAuthorizedForOffering(anyLong(), eq(2))).thenReturn(false);

        assertThatThrownBy(() -> service.medicalStudents(officer, 2)).isInstanceOf(AuthorizationException.class);
    }

    @Test
    void studentsCannotReadTheMedicalRoster() {
        assertThatThrownBy(() -> service.medicalStudents(student, 1)).isInstanceOf(AuthorizationException.class);
    }
}
