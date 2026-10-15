package lk.ac.ruhuna.fot.ams.api.response;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public record LoginResponse(
        long userId,
        String username,
        Role role,
        String displayName
) {}
