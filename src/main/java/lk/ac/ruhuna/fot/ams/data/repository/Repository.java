package lk.ac.ruhuna.fot.ams.data.repository;

import java.util.List;
import java.util.Optional;

public interface Repository<T> {
    Optional<T> findById(long id);
    List<T> findAll();
    T save(T entity);
}
