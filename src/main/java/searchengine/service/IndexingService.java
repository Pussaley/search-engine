package searchengine.service;

import searchengine.model.dto.response.Response;

public interface IndexingService {
    Response startIndexing();
    Response stopIndexing();
}