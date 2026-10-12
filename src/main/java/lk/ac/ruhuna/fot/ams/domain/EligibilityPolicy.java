package lk.ac.ruhuna.fot.ams.domain;

public interface EligibilityPolicy<T, R> {
    R evaluate(T input);
}
