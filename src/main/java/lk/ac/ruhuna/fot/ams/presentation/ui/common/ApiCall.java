package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.SwingWorker;
import lk.ac.ruhuna.fot.ams.api.controller.ApiError;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;

/** Runs a controller call off the Swing thread, then delivers the result or the safe error back on it. */
public final class ApiCall {
    private ApiCall() {
    }

    public static <T> void run(Supplier<ApiResponse<T>> call, Consumer<T> onSuccess, Consumer<ApiError> onFailure) {
        new SwingWorker<ApiResponse<T>, Void>() {
            @Override protected ApiResponse<T> doInBackground() {
                return call.get();
            }

            @Override protected void done() {
                try {
                    ApiResponse<T> response = get();
                    if (response.successful()) {
                        onSuccess.accept(response.data());
                    } else {
                        onFailure.accept(response.error());
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    onFailure.accept(new ApiError("INTERNAL_ERROR", "The operation could not be completed."));
                }
            }
        }.execute();
    }
}
