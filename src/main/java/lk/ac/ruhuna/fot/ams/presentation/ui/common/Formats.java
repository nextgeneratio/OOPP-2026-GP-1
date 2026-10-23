package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Display and input formats shared by screens. Parsing only turns typed text into a value; rules stay in services. */
public final class Formats {
    public static final String DATE_HINT = "yyyy-MM-dd";
    public static final String DATE_TIME_HINT = "yyyy-MM-dd HH:mm";
    public static final String TIME_HINT = "HH:mm";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(DATE_TIME_HINT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(TIME_HINT);

    private Formats() {
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String time(LocalTime value) {
        return value == null ? "" : TIME.format(value);
    }

    /** Each parser throws DateTimeParseException for bad text; screens turn that into a message near the field. */
    public static LocalDate parseDate(String text) throws DateTimeParseException {
        return LocalDate.parse(text.trim());
    }

    public static LocalDateTime parseDateTime(String text) throws DateTimeParseException {
        return LocalDateTime.parse(text.trim(), DATE_TIME);
    }

    public static LocalTime parseTime(String text) throws DateTimeParseException {
        return LocalTime.parse(text.trim(), TIME);
    }
}
