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
    public record CourseCatalog(
            List<CourseDao.DepartmentRow> departments,
            List<CourseDao.BatchRow> batches,
            List<CourseRow> courses,
            List<CourseDao.SemesterRow> semesters,
            List<OfferingRow> offerings,
            List<CourseDao.LecturerOption> lecturers,
            List<ComponentRow> components) {
        public CourseCatalog {
            departments = List.copyOf(departments);
            batches = List.copyOf(batches);
            courses = List.copyOf(courses);
            semesters = List.copyOf(semesters);
            offerings = List.copyOf(offerings);
            lecturers = List.copyOf(lecturers);
            components = List.copyOf(components);
        }
    }

    public record OfferingSetup(
            int batchId, int semesterId, Integer coordinatorId, String status, List<Integer> lecturerIds) {
        public OfferingSetup {
            lecturerIds = lecturerIds == null ? List.of() : List.copyOf(lecturerIds);
        }
    }

    /** UI-friendly offering data: users see a course name rather than opaque foreign-key IDs. */
    public record OfferingView(OfferingRow offering, CourseRow course, boolean enrolled,
            String academicYear, int semesterNumber) {
        public OfferingView(OfferingRow offering, CourseRow course, boolean enrolled) {
            this(offering, course, enrolled, "", 0);
        }
        public String semesterLabel() {
            return academicYear == null || academicYear.isBlank() || semesterNumber <= 0
                    ? "Academic term unavailable" : academicYear + " — Semester " + semesterNumber;
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
        requireCourseManager(actor);
        validateCourse(course, components);
        if (offeringSetup != null) {
            validateOfferingSetup(course, offeringSetup);
            authorizeOfferingSetup(actor, course, offeringSetup);
        }
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
        String code = department.code().trim().toUpperCase(java.util.Locale.ROOT);
        if (!List.of("ICT", "BST", "ET").contains(code)) {
            throw new ValidationException("Only ICT, BST, and ET departments are supported.");
        }
        return transactions.execute(connection -> {
            CourseDao.DepartmentRow normalized = new CourseDao.DepartmentRow(
                    department.id(), code, department.name().trim(), department.officeLocation(), department.active());
            long id = courseDao.insertDepartment(connection, normalized);
            audit(connection, actor.userId(), "DEPARTMENT_CREATE", "DEPARTMENT", id);
            return id;
        });
    }

    public long createBatch(AuthenticatedSession actor, CourseDao.BatchRow batch) {
        authorization.requireAdmin(actor);
        if (batch == null || batch.departmentId() <= 0 || batch.intakeYear() <= 0
                || batch.programme() == null || batch.programme().isBlank()
                || batch.code() == null || batch.code().isBlank()
                || batch.level() < 1 || batch.level() > 4) {
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
        requireCourseManager(actor);
        if (courseId <= 0 || setup == null || setup.batchId() <= 0 || setup.semesterId() <= 0
                || setup.status() == null || !List.of("OPEN", "CLOSED").contains(setup.status())) {
            throw new ValidationException("Enter a valid course offering.");
        }
        CourseRow course = courseDao.findCourse(courseId)
                .orElseThrow(() -> new ValidationException("Course was not found."));
        validateOfferingSetup(course, setup);
        authorizeOfferingSetup(actor, course, setup);
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
        requireCourseManager(actor);
        if (offeringId <= 0 || lecturerId <= 0) {
            throw new ValidationException("Offering and lecturer identifiers must be valid.");
        }
        OfferingRow offering = courseDao.findOffering(offeringId)
                .orElseThrow(() -> new ValidationException("Course offering was not found."));
        if (actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        }
        CourseRow course = courseDao.findCourse(offering.courseId())
                .orElseThrow(() -> new ValidationException("Course for this offering was not found."));
        requireLecturerInDepartment(lecturerId, course.departmentId());
        if (courseDao.findLecturersForOffering(offeringId).contains(lecturerId)) {
            throw new BusinessRuleException("This lecturer is already assigned to the course offering.");
        }
        transactions.execute(connection -> {
            courseDao.assignLecturer(connection, offeringId, lecturerId);
            audit(connection, actor.userId(), "LECTURER_ASSIGN", "COURSE_OFFERING", offeringId);
            return null;
        });
    }

    public List<CourseDao.LecturerOption> assignedLecturers(AuthenticatedSession actor, int offeringId) {
        requireCourseManager(actor);
        if (offeringId <= 0) throw new ValidationException("Choose a valid course offering.");
        if (actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        }
        java.util.Set<Integer> assigned = new java.util.HashSet<>(courseDao.findLecturersForOffering(offeringId));
        return courseDao.findLecturerOptions().stream().filter(lecturer -> assigned.contains(lecturer.id())).toList();
    }

    public List<OfferingRow> offeringsForActor(AuthenticatedSession actor) {
        if (actor == null) {
            throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException("Sign in to view offerings.");
        }
        return switch (actor.role()) {
            case ADMIN -> courseDao.findOfferings(null, null);
            case TECHNICAL_OFFICER -> {
                authorization.technicalOfficerId(actor);
                yield courseDao.findOfferingsForTechnicalOfficer(actor.userId());
            }
            case LECTURER -> courseDao.findOfferingsForLecturer(authorization.lecturerId(actor), null);
            case UNDERGRADUATE -> courseDao.findOfferingsForStudent(authorization.studentId(actor));
        };
    }

    public List<OfferingView> offeringViewsForActor(AuthenticatedSession actor) {
        List<OfferingRow> offerings = offeringsForActor(actor);
        java.util.Map<Integer, CourseDao.SemesterRow> semesters = courseDao.findSemesters().stream()
                .collect(java.util.stream.Collectors.toMap(CourseDao.SemesterRow::id, semester -> semester));
        return offerings.stream().map(offering -> {
            CourseDao.SemesterRow semester = semesters.get(offering.semesterId());
            return new OfferingView(offering,
                    courseDao.findCourse(offering.courseId()).orElseThrow(() ->
                            new NotFoundException("Course for an offering was not found.")), true,
                    semester == null ? "" : semester.academicYear(),
                    semester == null ? 0 : semester.semesterNumber());
        }).toList();
    }

    public List<OfferingView> availableOfferingsForCurrentStudent(AuthenticatedSession actor) {
        int batchId = authorization.studentBatchId(actor);
        int studentId = authorization.studentId(actor);
        java.util.Map<Integer, CourseDao.SemesterRow> semesters = courseDao.findSemesters().stream()
                .collect(java.util.stream.Collectors.toMap(CourseDao.SemesterRow::id, semester -> semester));
        return courseDao.findOfferings(batchId, null).stream()
                .filter(offering -> "OPEN".equals(offering.status()))
                .map(offering -> {
                    CourseDao.SemesterRow semester = semesters.get(offering.semesterId());
                    return new OfferingView(offering, courseDao.findCourse(offering.courseId())
                        .orElseThrow(() -> new NotFoundException("Course for an offering was not found.")),
                        courseDao.hasActiveEnrollment(studentId, offering.id()),
                        semester == null ? "" : semester.academicYear(),
                        semester == null ? 0 : semester.semesterNumber());
                })
                .toList();
    }

    public List<CourseDao.OfferingSummary> offeringSummariesForActor(AuthenticatedSession actor) {
        return offeringViewsForActor(actor).stream().map(view -> new CourseDao.OfferingSummary(
                view.offering().id(), view.course().id(), view.course().code(), view.course().title(),
                view.course().departmentId(), view.offering().semesterId(),
                view.academicYear(), view.semesterNumber())).toList();
    }

    public List<CourseDao.RosterRow> rosterForOffering(AuthenticatedSession actor, int offeringId) {
        if (actor == null) throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException(
                "A signed-in user is required to view an enrolment roster.");
        switch (actor.role()) {
            case LECTURER -> authorization.requireLecturerForOffering(actor, offeringId);
            case TECHNICAL_OFFICER -> authorization.requireTechnicalOfficerForOffering(actor, offeringId);
            case ADMIN -> authorization.requireAdmin(actor);
            case UNDERGRADUATE -> authorization.requireUndergraduateForOffering(actor, offeringId);
        }
        int ownStudentId = actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.UNDERGRADUATE
                ? authorization.studentId(actor) : -1;
        return courseDao.findRoster(offeringId).stream()
                .filter(row -> actor.role() != lk.ac.ruhuna.fot.ams.domain.enums.Role.UNDERGRADUATE
                        || row.studentId() == ownStudentId)
                .toList();
    }

    public long enrollCurrentStudent(AuthenticatedSession actor, int offeringId) {
        int studentId = authorization.studentId(actor);
        OfferingRow offering = courseDao.findOffering(offeringId)
                .orElseThrow(() -> new NotFoundException("Course offering was not found."));
        if (offering.batchId() != authorization.studentBatchId(actor)) {
            throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException(
                    "You may only register for offerings in your own batch.");
        }
        return enrollAs(actor, new CourseDao.EnrollmentRow(0, studentId, offeringId, 1, "NORMAL", "ACTIVE"));
    }

    public CourseCatalog catalog(AuthenticatedSession actor) {
        requireCourseManager(actor);
        if (actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.ADMIN) {
            List<CourseDao.DepartmentRow> departments = courseDao.findDepartments(false);
            List<CourseDao.BatchRow> batches = departments.stream()
                    .flatMap(department -> courseDao.findBatches(department.id()).stream()).toList();
            return new CourseCatalog(departments, batches, courseDao.searchCourses(null, null, false),
                    courseDao.findSemesters(), courseDao.findOfferings(null, null),
                    courseDao.findLecturerOptions(), courseDao.searchCourses(null, null, false).stream()
                            .flatMap(course -> courseDao.findComponents(course.id()).stream()).toList());
        }
        authorization.technicalOfficerId(actor);
        List<OfferingRow> offerings = courseDao.findOfferingsForTechnicalOfficer(actor.userId());
        java.util.Set<Integer> batchIds = offerings.stream().map(OfferingRow::batchId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<Integer> courseIds = offerings.stream().map(OfferingRow::courseId)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Set<Integer> semesterIds = offerings.stream().map(OfferingRow::semesterId)
                .collect(java.util.stream.Collectors.toSet());
        List<CourseDao.DepartmentRow> departments = courseDao.findDepartments(false).stream()
                .filter(department -> offerings.stream().anyMatch(offering -> courseDao.findBatches(
                        department.id()).stream().anyMatch(batch -> batch.id() == offering.batchId())))
                .toList();
        List<CourseDao.BatchRow> batches = departments.stream()
                .flatMap(department -> courseDao.findBatches(department.id()).stream())
                .filter(batch -> batchIds.contains(batch.id())).toList();
        return new CourseCatalog(departments, batches,
                courseDao.searchCourses(null, null, false).stream()
                        .filter(course -> courseIds.contains(course.id())).toList(),
                courseDao.findSemesters().stream().filter(semester -> semesterIds.contains(semester.id())).toList(),
                offerings, courseDao.findLecturerOptions().stream().filter(lecturer -> departments.stream()
                        .anyMatch(department -> department.code().equals(lecturer.departmentCode()))).toList(),
                courseIds.stream().flatMap(id -> courseDao.findComponents(id).stream()).toList());
    }

    public List<CourseDao.SemesterRow> semestersForActor(AuthenticatedSession actor) {
        if (actor == null) throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException(
                "Sign in to view academic terms.");
        return courseDao.findSemesters();
    }

    public long enroll(
            AuthenticatedSession actor,
            CourseDao.EnrollmentRow enrollment) {
        authorization.requireAdmin(actor);
        return enrollAs(actor, enrollment);
    }

    private long enrollAs(AuthenticatedSession actor, CourseDao.EnrollmentRow enrollment) {
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

    private void requireCourseManager(AuthenticatedSession actor) {
        authorization.requireRole(actor, lk.ac.ruhuna.fot.ams.domain.enums.Role.ADMIN,
                lk.ac.ruhuna.fot.ams.domain.enums.Role.TECHNICAL_OFFICER);
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

    private void validateOfferingSetup(CourseRow course, OfferingSetup setup) {
        if (course == null || setup == null) throw new ValidationException("Choose a course and offering details.");
        boolean matchingBatch = courseDao.findBatches(course.departmentId()).stream()
                .anyMatch(batch -> batch.id() == setup.batchId());
        boolean matchingSemester = courseDao.findSemesters().stream()
                .anyMatch(semester -> semester.id() == setup.semesterId());
        if (!matchingBatch || !matchingSemester) {
            throw new ValidationException("The batch must belong to the course department, and the semester must exist.");
        }
        LinkedHashSet<Integer> lecturerIds = new LinkedHashSet<>(setup.lecturerIds());
        if (setup.coordinatorId() != null) lecturerIds.add(setup.coordinatorId());
        for (Integer lecturerId : lecturerIds) {
            if (lecturerId == null || lecturerId <= 0) throw new ValidationException("Choose valid lecturer assignments.");
            requireLecturerInDepartment(lecturerId, course.departmentId());
        }
    }

    private void authorizeOfferingSetup(AuthenticatedSession actor, CourseRow course, OfferingSetup setup) {
        if (actor.role() != lk.ac.ruhuna.fot.ams.domain.enums.Role.TECHNICAL_OFFICER) return;
        CourseDao.BatchRow batch = courseDao.findBatches(course.departmentId()).stream()
                .filter(row -> row.id() == setup.batchId()).findFirst()
                .orElseThrow(() -> new ValidationException("The batch is not in the authorized course department."));
        if (batch.departmentId() != course.departmentId()) {
            throw new ValidationException("The batch must belong to the selected course department.");
        }
        authorization.requireTechnicalOfficerForTimetable(actor, batch.departmentId(), setup.semesterId());
    }

    private void requireLecturerInDepartment(int lecturerId, int departmentId) {
        boolean valid = courseDao.findLecturerOptions().stream().anyMatch(lecturer -> lecturer.id() == lecturerId
                && lecturer.departmentId() == departmentId);
        if (!valid) throw new ValidationException("The lecturer must be active and belong to the course department.");
    }

    private void audit(
            java.sql.Connection connection, Long userId, String action, String entity, long entityId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection,
                new AuditLogDao.AuditEvent(0, userId, action, entity, entityId, LocalDateTime.now(clock)));
    }
}
