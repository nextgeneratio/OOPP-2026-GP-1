# Timetable redesign progress — 2026-11-05

Implemented the Admin weekly timetable workflow with a single create/update editor for department, term, level and repeated day/time/location rows. Added four Admin actions, weekly list view, role-scoped read-only screens, soft deactivation/versioning, generic nullable offering/component slot support, attendance scope/weekday validation, migration SQL and mocked service tests.

Validation so far: `mvn -q -DskipTests compile` passed; targeted `WeeklyTimetableServiceTest` passed (4 tests). Full clean test/verify/package and diff checks remain to be run. The SQL migration and configured application database were not executed or modified. GUI acceptance is pending.
