package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.UserService;
import lk.ac.ruhuna.fot.ams.business.service.UserService.CreateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UpdateUser;
import lk.ac.ruhuna.fot.ams.business.service.UserService.UserView;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class UserController {
    public record SearchUsersRequest(String term, Role role, Boolean active) {
    }

    public record UserIdResponse(long userId) {
    }

    private final UserService service;
    private final ApiControllerSupport support;

    public UserController(UserService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<UserIdResponse> create(AuthenticatedSession actor, CreateUser request) {
        return support.execute(request, () -> new UserIdResponse(service.create(actor, request)));
    }

    public ApiResponse<Void> update(AuthenticatedSession actor, UpdateUser request) {
        return support.execute(request, () -> {
            service.update(actor, request);
            return null;
        });
    }

    public ApiResponse<Void> deactivate(AuthenticatedSession actor, long userId) {
        return support.execute(userId, () -> {
            service.deactivate(actor, userId);
            return null;
        });
    }

    public ApiResponse<List<UserView>> search(AuthenticatedSession actor, SearchUsersRequest request) {
        return support.execute(() -> service.search(actor,
                request == null ? null : request.term(),
                request == null ? null : request.role(),
                request == null ? null : request.active()));
    }
}
