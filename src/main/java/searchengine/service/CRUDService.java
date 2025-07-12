package searchengine.service;

import java.util.Optional;

public interface CRUDService<T> {
    Optional<T> findById(Long id);
    void deleteById(Long id);
    T save(T item);
}