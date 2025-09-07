package searchengine.service;

import java.util.concurrent.ExecutionException;

public interface IndexingService<T> {
    T startIndexing();
    T stopIndexing() throws ExecutionException, InterruptedException;
}