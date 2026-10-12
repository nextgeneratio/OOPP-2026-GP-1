package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class Course {
    private final String code;
    private final String title;
    private final double credits;

    public Course(String code, String title, double credits) {
        if (code == null || code.isBlank() || title == null || title.isBlank()) {
            throw new ValidationException("Course code and title are required.");
        }
        if (Double.isNaN(credits) || credits <= 0) {
            throw new ValidationException("Credit value must be greater than zero.");
        }
        this.code = code;
        this.title = title;
        this.credits = credits;
    }

    public String code() { return code; }
    public String title() { return title; }
    public double credits() { return credits; }
}
