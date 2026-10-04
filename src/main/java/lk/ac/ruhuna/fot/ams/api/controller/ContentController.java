package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.ContentService;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.MaterialRow;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.NoticeRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ContentController {
    public record MaterialRequest(int offeringId, String title, String filePath) {
    }

    public record UpdateMaterialRequest(
            int offeringId, int materialId, String title, String filePath) {
    }

    public record ContentIdResponse(long id) {
    }

    private final ContentService service;
    private final ApiControllerSupport support;

    public ContentController(ContentService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<ContentIdResponse> addMaterial(
            AuthenticatedSession actor, MaterialRequest request) {
        return support.execute(request, () -> new ContentIdResponse(service.addMaterial(
                actor, request.offeringId(), request.title(), request.filePath())));
    }

    public ApiResponse<Void> updateMaterial(
            AuthenticatedSession actor, UpdateMaterialRequest request) {
        return support.execute(request, () -> {
            service.updateMaterial(actor, request.offeringId(), request.materialId(),
                    request.title(), request.filePath());
            return null;
        });
    }

    public ApiResponse<List<MaterialRow>> materialsForStudent(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.materialsForStudent(actor, offeringId));
    }

    public ApiResponse<List<MaterialRow>> materialsForLecturer(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.materialsForLecturer(actor, offeringId));
    }

    public ApiResponse<ContentIdResponse> createNotice(
            AuthenticatedSession actor, NoticeRow request) {
        return support.execute(request, () -> new ContentIdResponse(service.createNotice(actor, request)));
    }

    public ApiResponse<List<NoticeRow>> visibleNotices(AuthenticatedSession actor) {
        return support.execute(() -> service.visibleNotices(actor));
    }
}
