package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class Assessment {
    private final String name;
    private double weightPercent;
    private final boolean caComponent;

    public Assessment(String name, double weightPercent, boolean caComponent) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Assessment name is required.");
        }
        this.name = name;
        this.caComponent = caComponent;
        setWeight(weightPercent);
    }

    public String name() { return name; }
    public double weightPercent() { return weightPercent; }
    public boolean isCaComponent() { return caComponent; }

    public void setWeight(double weightPercent) {
        if (Double.isNaN(weightPercent) || weightPercent < 0 || weightPercent > 100) {
            throw new ValidationException("Assessment weight must be between 0 and 100.");
        }
        this.weightPercent = weightPercent;
    }
}
