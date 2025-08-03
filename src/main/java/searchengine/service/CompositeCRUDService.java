package searchengine.service;

import searchengine.service.crud.CRUDService;

import java.util.Optional;

public interface CompositeCRUDService<T> extends CRUDService<T> {
    Optional<T> findByPageIdAndLemmaId(Long pageId, Long lemmaId);
}