package searchengine.service;

import java.util.Optional;

public interface CompositeCRUDService<T> extends CRUDService<T> {
    Optional<T> findByPageIdAndLemmaId(Long pageId, Long lemmaId);
}