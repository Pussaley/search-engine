package searchengine.service;

import searchengine.model.dto.response.Response;

public interface IndexingService<T> {
    T startIndexing();
    T stopIndexing();
}