package lk.ac.ruhuna.fot.ams.api.controller;

public record ApiResponse<T>(boolean successful, T data, ApiError error) {
    public ApiResponse {
        if (successful ? error != null : error == null || data != null) {
            throw new IllegalArgumentException("A response must contain either success data or an error.");
        }
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
