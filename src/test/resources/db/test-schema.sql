-- Integration tests use a dedicated database configured by test-application.properties.
-- The production schema is intentionally kept as the single authoritative DDL source
-- and is applied to the test database by the test setup before integration tests run.
CREATE DATABASE IF NOT EXISTS `university_management_test`;
USE `university_management_test`;
