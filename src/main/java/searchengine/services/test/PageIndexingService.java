package searchengine.services.test;

import searchengine.dto.entity.PageDTO;

public interface PageIndexingService<T> extends IndexingService<T>{
    void startIndexing();
}
