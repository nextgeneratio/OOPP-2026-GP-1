package lk.ac.ruhuna.fot.ams.api.request;

public record LoginRequest(
        String username,
        char[] password
) {}
