package lk.ac.ruhuna.fot.ams.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.Properties;

public final class AppConfig {
    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() throws IOException {
        Properties properties = new Properties();
        Path localFile = Path.of("application.properties");
        if (Files.exists(localFile)) {
            try (InputStream input = Files.newInputStream(localFile)) {
                properties.load(input);
            }
        } else {
            try (InputStream input = AppConfig.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (input != null) {
                    properties.load(input);
                }
            }
        }
        return new AppConfig(properties);
    }

    public static AppConfig loadFromClasspath(String resourceName) throws IOException {
        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("Configuration resource name is required.");
        }
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Configuration resource not found: " + resourceName);
            }
            Properties properties = new Properties();
            properties.load(input);
            return new AppConfig(properties);
        }
    }

    public String environment() {
        return value("app.environment", "development");
    }

    public String databaseUrl() {
        return required("db.url", "DB_URL");
    }

    public String databaseUsername() {
        return required("db.username", "DB_USERNAME");
    }

    public String databasePassword() {
        return required("db.password", "DB_PASSWORD");
    }

    public int poolSize() {
        return Integer.parseInt(value("db.pool-size", "5"));
    }

    public BigDecimal attendanceThreshold() {
        return new BigDecimal(value("academic.attendance-threshold", "80.00"));
    }

    public String repeatSelectionRule() {
        return value("academic.cgpa-repeat-selection", "LATEST_COMPLETED_ATTEMPT");
    }

    public String gradeSchemeVersion() {
        return value("academic.grade-scheme-version", "UGC-2024-v1");
    }

    private String required(String property, String environmentVariable) {
        String configured = value(property, null);
        if (configured == null || configured.isBlank() || configured.startsWith("your_")) {
            configured = System.getenv(environmentVariable);
        }
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing configuration: " + property);
        }
        return configured;
    }

    private String value(String property, String defaultValue) {
        String environmentVariable = property.toUpperCase().replace('.', '_').replace('-', '_');
        String configured = System.getenv(environmentVariable);
        return configured != null && !configured.isBlank()
                ? configured
                : properties.getProperty(property, defaultValue);
    }
}
