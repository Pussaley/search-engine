package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.demo.ResponseErrorMessageDto;
import searchengine.model.dto.response.demo.ResponseSuccessMessageDto;
import searchengine.service.IndexingService;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.service.recursive.JsoupSiteIndexingResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class IndexingServiceImpl implements IndexingService<Response> {
    private final SitesList sites;
    private final SiteServiceImpl siteService;
    private ForkJoinPool forkJoinPool;
    private ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);


    @Override
    public Response startIndexing() {

        if (isRunning.compareAndSet(false, true)) {
            log.info("Запускаем индексацию.");
            forkJoinPool = new ForkJoinPool();
            List<Site> siteList = sites.getSites();
            executorService = Executors.newFixedThreadPool(siteList.size());

            executorService.submit(() -> siteList.forEach(site -> executorService.submit(
                            () -> {
                                try {
                                    siteService.clearDatabaseBySiteName(site.getName());
                                    SiteDto savedSite = siteService.save(site);

                                    ForkJoinRecursiveTask recursiveTask = new ForkJoinRecursiveTask(site);

                                    log.info("[Time: {}] - Запущена индексация сайта {}.", LocalDateTime.now(), site.getName());
                                    JsoupSiteIndexingResponse indexingResult = forkJoinPool.invoke(recursiveTask);
                                    log.info("Индексация сайта {} завершена.", site.getName());

                                    SiteStatus status = indexingResult.getStatus();
                                    String errorMessage = indexingResult.getError();

                                    savedSite.setSiteStatus(status);
                                    savedSite.setLastError(errorMessage);
                                    savedSite.setStatusTime(LocalDateTime.now());

                                    siteService.updateSite(savedSite);

                                } catch (Exception e) {
                                    log.error("Ошибка типа {} во время индексации сайта {}", e.getClass(), site.getName());
                                }
                            }
                    )
            ));

            return new ResponseSuccessMessageDto(isRunning.get());
        } else {
            log.info("Индексация уже запущена");
            return new ResponseErrorMessageDto(false, "Индексация уже запущена");
        }
    }

    @Override
    public Response stopIndexing() {
        if (!isRunning.get()) {
            log.info("Индексация не запущена");
            return new ResponseErrorMessageDto(isRunning.get(), "Индексация не запущена");
        }

        int defaultTimeOut = 60;
        boolean terminated = false;
        try {
            forkJoinPool.shutdown();
            log.info("Индексация была остановлена пользователем");
            terminated = forkJoinPool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("Ожидание завершения FJP было прервано <{}>", e.getMessage());
        } finally {
            if (!forkJoinPool.isTerminated() || !terminated) {
                try {
                    log.info("Повторная попытка завершить индексацию");
                    forkJoinPool.shutdownNow();
                    forkJoinPool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.info("Повторная попытка завершить индексацию была прервана");
                    Thread.currentThread().interrupt();
                }
            }

            executorService.submit(
                    () -> siteService.updateNotIndexedEntitiesAfterStoppingIndexing(
                            SiteStatus.FAILED, "Индексация была остановлена пользователем"));
        }
        return new ResponseSuccessMessageDto(isRunning.getAndSet(false));
    }
}