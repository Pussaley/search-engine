package searchengine.services.recursive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
public class RecursiveActionImpl implements RecursiveAction {
    private final RecursiveActionHandler handler;

    @Override
    public void start() {
        try {
            ForkJoinPool commonPool = ForkJoinPool.commonPool();
            commonPool.invoke(handler);
        } catch (Exception exception) {
            log.error("Exception: {}", exception.getMessage());
            exception.printStackTrace();
        }
    }
}