package searchengine.service.recursive;

import lombok.extern.slf4j.Slf4j;
import searchengine.service.impl.PageServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class FJPDemo {
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private ForkJoinPool forkJoinPool;
    private List<ForkJoinTask<Boolean>> tasks = new ArrayList<>();

    public boolean startFJP() {
        if (forkJoinPool == null) {
            forkJoinPool = new ForkJoinPool();
        }
        isRunning.compareAndSet(false, true);
        if (isRunning.get()) {
            if (!tasks.isEmpty()) {
                List<Boolean> result = tasks.stream().map(forkJoinPool::invoke).toList();
                log.info("Индексация запущена");
                return result.parallelStream().allMatch((r) -> r);
            }
        } else {
            log.info("Индексация уже запущена");
        }
        return false;
    }

    public boolean stopFJP() {
        if (forkJoinPool != null && isRunning.compareAndSet(true, false)) {
            int defaultTimeOut = 5;
            try {
                forkJoinPool.shutdownNow();
                log.info("Индексация была остановлена пользователем");
                forkJoinPool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("Ожидание завершения FJP было прервано <{}>", e.getMessage());
                Thread.currentThread().interrupt();
            }
            return forkJoinPool.isTerminated();
        } else {
            log.info("Индексация по каким-то причинам не запущена, скорее всего завершилась");
            return false;
        }
    }

    public void addTask(ForkJoinTask<Boolean> task) {
        this.tasks.add(task);
    }
    public void deleteTask(ForkJoinTask<Boolean> task) {
        this.tasks.remove(task);
    }

    public boolean getStatus() {
        return isRunning.get();
    }
}