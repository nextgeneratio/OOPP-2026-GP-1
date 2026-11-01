package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import lk.ac.ruhuna.fot.ams.api.controller.ApiControllerSupport;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import org.junit.jupiter.api.Test;

class DatabaseFailureReportingTest {
    @Test
    void controllerReturnsADatabaseFailureInsteadOfSuccessfulEmptyData() {
        ApiControllerSupport controller = new ApiControllerSupport(new ApplicationErrorHandler());

        var response = controller.execute(() -> {
            throw new DataAccessException("Unable to read application data.",
                    new SQLException("Synthetic connection failure", "08001"));
        });

        assertThat(response.successful()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("DATA_ACCESS_ERROR");
        assertThat(response.error().message()).contains("database", "SQL state 08001", "vendor code 0")
                .doesNotContain("password", "Synthetic connection failure");
    }

    @Test
    void missingDatabaseColumnIsReportedAsSchemaUpdateRequired() {
        ApiControllerSupport controller = new ApiControllerSupport(new ApplicationErrorHandler());
        var response = controller.execute(() -> {
            throw new DataAccessException("Unable to read application data.",
                    new SQLException("Unknown column", "42S22", 1054));
        });
        assertThat(response.successful()).isFalse();
        assertThat(response.error().message()).contains("schema is missing a column", "compatibility SQL");
    }

    @Test
    void missingDatabaseTableIsReportedAsSchemaUpdateRequired() {
        ApiControllerSupport controller = new ApiControllerSupport(new ApplicationErrorHandler());
        var response = controller.execute(() -> {
            throw new DataAccessException("Unable to read application data.",
                    new SQLException("Table missing", "42S02", 1146));
        });
        assertThat(response.successful()).isFalse();
        assertThat(response.error().message()).contains("schema is missing a table", "compatibility SQL");
    }
}
