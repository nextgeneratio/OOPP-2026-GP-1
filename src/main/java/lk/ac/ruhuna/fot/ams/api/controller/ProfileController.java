package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.ProfileService;
import lk.ac.ruhuna.fot.ams.business.service.ProfileService.ProfileView;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ProfileController {
    public record StaffProfileRequest(String email, String fullName, String phone) {
    }

    public record UndergraduateProfileRequest(String email, String profilePictureReference) {
    }

    public record ChangePasswordRequest(char[] currentPassword, char[] newPassword, char[] confirmation) {
    }

    private final ProfileService service;
    private final ApiControllerSupport support;

    public ProfileController(ProfileService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<ProfileView> ownProfile(AuthenticatedSession actor) {
        return support.execute(() -> service.ownProfile(actor));
    }

    public ApiResponse<Void> updateStaff(
            AuthenticatedSession actor, StaffProfileRequest request) {
        return support.execute(request, () -> {
            service.updateStaffProfile(actor, request.email(), request.fullName(), request.phone());
            return null;
        });
    }

    public ApiResponse<Void> updateUndergraduate(
            AuthenticatedSession actor, UndergraduateProfileRequest request) {
        return support.execute(request, () -> {
            service.updateUndergraduateProfile(
                    actor, request.email(), request.profilePictureReference());
            return null;
        });
    }

    public ApiResponse<Void> changePassword(
            AuthenticatedSession actor, ChangePasswordRequest request) {
        return support.execute(request, () -> {
            service.changePassword(actor, request.currentPassword(),
                    request.newPassword(), request.confirmation());
            return null;
        });
    }
}
