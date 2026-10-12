package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class Mark {
    private double score;

    public Mark(double score) {
        setScore(score);
    }

    public double score() { return score; }

    public void setScore(double score) {
        if (Double.isNaN(score) || score < 0 || score > 100) {
            throw new ValidationException("Marks must be between 0 and 100.");
        }
        this.score = score;
    }
}
