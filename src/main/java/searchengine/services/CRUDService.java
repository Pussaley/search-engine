package searchengine.services;

import java.util.Optional;

public interface CRUDService<T> {
    Optional<T> getById(Long id);
    boolean deleteById(Long id);
    T save(T item);
}