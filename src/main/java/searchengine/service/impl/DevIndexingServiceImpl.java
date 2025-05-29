package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.ResponseSuccessDto;
import searchengine.service.IndexingService;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.service.recursive.ForkJoinRecursiveTaskDemo;
import searchengine.util.jsoup.JSOUPParser;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("dev")
public class DevIndexingServiceImpl implements IndexingService<Response> {
    private final SitesList sites;
    private final SiteServiceImpl siteService;
    private ForkJoinPool forkJoinPool;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public Response startIndexing() {
        if (forkJoinPool == null || forkJoinPool.isTerminated()) {
            forkJoinPool = new ForkJoinPool();
            log.info("FJP инициализирован");
            isRunning.compareAndSet(false, true);
            log.info("isRunning is {}", isRunning.get());
        }

        new Thread(() -> sites.getSites().forEach(
                (site) -> {
                    siteService.clearDatabaseBySiteName(site.getName());

                    SiteDto savedSiteDto = siteService.save(SiteDto
                            .builder()
                            .url(site.getUrl())
                            .name(site.getName())
                            .statusTime(LocalDateTime.now())
                            .siteStatus(SiteStatus.INDEXING)
                            .build());

                    log.info("Запущена индексация сайта {}", site.getName());

                    ForkJoinRecursiveTaskDemo recursiveTaskDemo = new ForkJoinRecursiveTaskDemo(site);
                    boolean resultOfIndexing = forkJoinPool.invoke(recursiveTaskDemo);

/*                            final JSOUPParser jsoupParser =
                                    SearchEngineApplicationContext.getBean(JSOUPParser.class);

                            Connection.Response response = jsoupParser.execute(site.getUrl());
                            Collection<String> links = jsoupParser.parseAbsoluteLinks(response);

                            ForkJoinRecursiveTask recursiveTask = new ForkJoinRecursiveTask(response, links);
                            boolean resultOfIndexing = forkJoinPool.invoke(recursiveTask);*/

                    SiteStatus status = resultOfIndexing ? SiteStatus.INDEXED : SiteStatus.FAILED;

                    savedSiteDto.setSiteStatus(status);
                    siteService.updateSite(savedSiteDto);
                })).start();

        return new ResponseSuccessDto(true);
    }

    @Override
    public Response stopIndexing() {

        new Thread(() -> {
            if (forkJoinPool != null && isRunning.compareAndSet(true, false)) {

                int defaultTimeOut = 20;

                try {
                    forkJoinPool.shutdownNow();
                    log.info("Индексация была остановлена пользователем");
                    forkJoinPool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.info("Ожидание завершения FJP было прервано <{}>", e.getMessage());
                    Thread.currentThread().interrupt();
                }
            } else {
                log.info("Индексация по каким-то причинам не запущена, скорее всего завершилась");
            }
        }).start();

        return new ResponseSuccessDto(true);
    }
}