package searchengine.service;

import java.util.Optional;

public interface CRUDService<T, ID> {
    Optional<T> findById(ID id);
    void deleteById(ID id);
    T save(T item);
}