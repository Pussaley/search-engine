package searchengine.service;

import searchengine.model.dto.response.Response;

import java.util.concurrent.ExecutionException;

public interface IndexingService<T> {
    T startIndexing();
    T stopIndexing() throws ExecutionException, InterruptedException;
}