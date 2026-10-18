package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.CourseService;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingSetup;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class CourseController {
    public record CreateCourseRequest(
            CourseDao.CourseRow course, List<CourseDao.ComponentRow> components, OfferingSetup offering) {
    }

    public record CreateOfferingRequest(int courseId, OfferingSetup setup) {
    }

    public record EnrollmentRequest(CourseDao.EnrollmentRow enrollment) {
    }

    public record LecturerAssignmentRequest(int offeringId, int lecturerId) {
    }

    public record IdResponse(long id) {
    }

    private final CourseService service;
    private final ApiControllerSupport support;

    public CourseController(CourseService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<IdResponse> createCourse(AuthenticatedSession actor, CreateCourseRequest request) {
        return support.execute(request, () -> new IdResponse(service.createCourse(
                actor, request.course(), request.components(), request.offering())));
    }

    public ApiResponse<IdResponse> createDepartment(
            AuthenticatedSession actor, CourseDao.DepartmentRow request) {
        return support.execute(request, () -> new IdResponse(service.createDepartment(actor, request)));
    }

    public ApiResponse<IdResponse> createBatch(AuthenticatedSession actor, CourseDao.BatchRow request) {
        return support.execute(request, () -> new IdResponse(service.createBatch(actor, request)));
    }

    public ApiResponse<IdResponse> createSemester(AuthenticatedSession actor, CourseDao.SemesterRow request) {
        return support.execute(request, () -> new IdResponse(service.createSemester(actor, request)));
    }

    public ApiResponse<IdResponse> createOffering(
            AuthenticatedSession actor, CreateOfferingRequest request) {
        return support.execute(request, () -> new IdResponse(service.createOffering(
                actor, request.courseId(), request.setup())));
    }

    public ApiResponse<IdResponse> enroll(AuthenticatedSession actor, EnrollmentRequest request) {
        return support.execute(request, () -> new IdResponse(
                service.enroll(actor, request.enrollment())));
    }

    public ApiResponse<Void> assignLecturer(
            AuthenticatedSession actor, LecturerAssignmentRequest request) {
        return support.execute(request, () -> {
            service.assignLecturer(actor, request.offeringId(), request.lecturerId());
            return null;
        });
    }

    public ApiResponse<Void> deactivateCourse(AuthenticatedSession actor, int courseId) {
        return support.execute(() -> {
            service.deactivateCourse(actor, courseId);
            return null;
        });
    }
}
