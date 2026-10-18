package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.ComponentRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.CourseRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class CourseService {
    public record OfferingSetup(
            int batchId, int semesterId, Integer coordinatorId, String status, List<Integer> lecturerIds) {
        public OfferingSetup {
            lecturerIds = lecturerIds == null ? List.of() : List.copyOf(lecturerIds);
        }
    }

    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;

    public CourseService(
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
    }

    public int createCourse(
            AuthenticatedSession actor,
            CourseRow course,
            List<ComponentRow> components,
            OfferingSetup offeringSetup) {
        authorization.requireAdmin(actor);
        validateCourse(course, components);
        return transactions.execute(connection -> {
            int courseId = Math.toIntExact(courseDao.insertCourse(connection, course));
            for (ComponentRow component : components) {
                courseDao.insertComponent(connection, new ComponentRow(
                        component.id(), courseId, component.type(), component.credits(), component.plannedSessions()));
            }
            audit(connection, actor.userId(), "COURSE_CREATE", "COURSE", courseId);
            if (offeringSetup != null) {
                createOffering(connection, actor, courseId, offeringSetup);
            }
            return courseId;
        });
    }

    public long createDepartment(AuthenticatedSession actor, CourseDao.DepartmentRow department) {
        authorization.requireAdmin(actor);
        if (department == null || department.code() == null || department.code().isBlank()
                || department.name() == null || department.name().isBlank()) {
            throw new ValidationException("Department code and name are required.");
        }
        return transactions.execute(connection -> {
            long id = courseDao.insertDepartment(connection, department);
            audit(connection, actor.userId(), "DEPARTMENT_CREATE", "DEPARTMENT", id);
            return id;
        });
    }

    public long createBatch(AuthenticatedSession actor, CourseDao.BatchRow batch) {
        authorization.requireAdmin(actor);
        if (batch == null || batch.departmentId() <= 0 || batch.intakeYear() <= 0
                || batch.programme() == null || batch.programme().isBlank()
                || batch.code() == null || batch.code().isBlank()) {
            throw new ValidationException("Batch details are invalid.");
        }
        return transactions.execute(connection -> {
            long id = courseDao.insertBatch(connection, batch);
            audit(connection, actor.userId(), "BATCH_CREATE", "BATCH", id);
            return id;
        });
    }

    public long createSemester(AuthenticatedSession actor, CourseDao.SemesterRow semester) {
        authorization.requireAdmin(actor);
        if (semester == null || semester.academicYear() == null || semester.academicYear().isBlank()
                || semester.semesterNumber() < 1 || semester.semesterNumber() > 2
                || semester.start() == null || semester.end() == null
                || !semester.end().isAfter(semester.start())) {
            throw new ValidationException("Semester dates or number are invalid.");
        }
        return transactions.execute(connection -> {
            long id = courseDao.insertSemester(connection, semester);
            audit(connection, actor.userId(), "SEMESTER_CREATE", "SEMESTER", id);
            return id;
        });
    }

    public int createOffering(AuthenticatedSession actor, int courseId, OfferingSetup setup) {
        authorization.requireAdmin(actor);
        if (courseId <= 0 || setup == null || setup.batchId() <= 0 || setup.semesterId() <= 0
                || setup.status() == null || !List.of("OPEN", "CLOSED").contains(setup.status())) {
            throw new ValidationException("Enter a valid course offering.");
        }
        return transactions.execute(connection -> createOffering(connection, actor, courseId, setup));
    }

    public void updateCourse(AuthenticatedSession actor, CourseRow course) {
        authorization.requireAdmin(actor);
        if (course == null || course.id() <= 0) {
            throw new ValidationException("Course identifier is invalid.");
        }
        validateCourse(course, courseDao.findComponents(course.id()));
        transactions.execute(connection -> {
            int updated = courseDao.updateCourse(connection, course);
            if (updated == 0) {
                throw new ValidationException("Course was not found.");
            }
            audit(connection, actor.userId(), "COURSE_UPDATE", "COURSE", course.id());
            return null;
        });
    }

    public void deactivateCourse(AuthenticatedSession actor, int courseId) {
        authorization.requireAdmin(actor);
        CourseRow course = courseDao.searchCourses(null, null, false).stream()
                .filter(row -> row.id() == courseId)
                .findFirst()
                .orElseThrow(() -> new ValidationException("Course was not found."));
        CourseRow inactive = new CourseRow(course.id(), course.departmentId(), course.code(),
                course.title(), course.credits(), false);
        transactions.execute(connection -> {
            courseDao.updateCourse(connection, inactive);
            audit(connection, actor.userId(), "COURSE_DEACTIVATE", "COURSE", courseId);
            return null;
        });
    }

    public void assignLecturer(AuthenticatedSession actor, int offeringId, int lecturerId) {
        authorization.requireAdmin(actor);
        if (offeringId <= 0 || lecturerId <= 0) {
            throw new ValidationException("Offering and lecturer identifiers must be valid.");
        }
        transactions.execute(connection -> {
            courseDao.assignLecturer(connection, offeringId, lecturerId);
            audit(connection, actor.userId(), "LECTURER_ASSIGN", "COURSE_OFFERING", offeringId);
            return null;
        });
    }

    public long enroll(
            AuthenticatedSession actor,
            CourseDao.EnrollmentRow enrollment) {
        authorization.requireAdmin(actor);
        if (enrollment == null || enrollment.studentId() <= 0 || enrollment.offeringId() <= 0
                || enrollment.attemptNumber() <= 0
                || enrollment.studentStatus() == null || enrollment.enrollmentStatus() == null
                || !List.of("NORMAL", "REPEAT", "BATCH_MISSED").contains(enrollment.studentStatus())
                || !List.of("ACTIVE", "COMPLETED", "WITHDRAWN").contains(enrollment.enrollmentStatus())
                || (enrollment.attemptNumber() == 1 && !enrollment.studentStatus().equals("NORMAL"))
                || (enrollment.attemptNumber() > 1 && enrollment.studentStatus().equals("NORMAL"))) {
            throw new ValidationException("Enter a valid course enrolment attempt.");
        }
        CourseDao.OfferingRow offering = courseDao.findOffering(enrollment.offeringId())
                .orElseThrow(() -> new NotFoundException("Course offering was not found."));
        if (!"OPEN".equals(offering.status())) {
            throw new BusinessRuleException("Students cannot be enrolled in a closed course offering.");
        }
        if ("ACTIVE".equals(enrollment.enrollmentStatus())
                && courseDao.hasActiveEnrollment(enrollment.studentId(), enrollment.offeringId())) {
            throw new BusinessRuleException("Student already has an active enrolment for this offering.");
        }
        return transactions.execute(connection -> {
            long enrollmentId = courseDao.insertEnrollment(connection, enrollment);
            audit(connection, actor.userId(), "ENROLLMENT_CREATE", "COURSE_ENROLLMENT", enrollmentId);
            return enrollmentId;
        });
    }

    private int createOffering(
            java.sql.Connection connection,
            AuthenticatedSession actor,
            int courseId,
            OfferingSetup setup) throws java.sql.SQLException {
        if (setup == null || setup.batchId() <= 0 || setup.semesterId() <= 0
                || setup.status() == null || !List.of("OPEN", "CLOSED").contains(setup.status())) {
            throw new ValidationException("Enter a valid course offering.");
        }
        int offeringId = Math.toIntExact(courseDao.insertOffering(connection,
                new OfferingRow(0, courseId, setup.batchId(), setup.semesterId(),
                        setup.coordinatorId(), setup.status())));
        LinkedHashSet<Integer> lecturerIds = new LinkedHashSet<>(setup.lecturerIds());
        if (setup.coordinatorId() != null) {
            lecturerIds.add(setup.coordinatorId());
        }
        for (Integer lecturerId : lecturerIds) {
            if (lecturerId == null || lecturerId <= 0) {
                throw new ValidationException("Lecturer assignments must be valid.");
            }
            courseDao.assignLecturer(connection, offeringId, lecturerId);
        }
        audit(connection, actor.userId(), "OFFERING_CREATE", "COURSE_OFFERING", offeringId);
        return offeringId;
    }

    private void validateCourse(CourseRow course, List<ComponentRow> components) {
        if (course == null || course.departmentId() <= 0 || course.code() == null || course.code().isBlank()
                || course.title() == null || course.title().isBlank()
                || !Double.isFinite(course.credits()) || course.credits() <= 0
                || components == null || components.isEmpty()) {
            throw new ValidationException("Course details and at least one component are required.");
        }
        if (components.stream().anyMatch(component -> component == null
                || component.type() == null || !Double.isFinite(component.credits())
                || component.credits() <= 0 || component.plannedSessions() <= 0)) {
            throw new ValidationException("Course components must have valid types, credits, and planned sessions.");
        }
        long distinctTypes = components.stream().map(ComponentRow::type).distinct().count();
        if (distinctTypes != components.size()) {
            throw new ValidationException("A course may have only one component of each type.");
        }
    }

    private void audit(
            java.sql.Connection connection, Long userId, String action, String entity, long entityId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection,
                new AuditLogDao.AuditEvent(0, userId, action, entity, entityId, LocalDateTime.now(clock)));
    }
}
