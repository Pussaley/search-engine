package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.props.concurrency.ConcurrencyProperties;
import searchengine.model.SiteStatus;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.demo.ResponseErrorMessageDto;
import searchengine.model.dto.response.demo.ResponseSuccessMessageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.IndexingService;
import searchengine.service.demo.SitePageServiceTest;
import searchengine.service.recursive.RecursiveSiteCrawler;
import searchengine.util.jsoup.JSOUPParser;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class IndexingServiceImpl implements IndexingService<Response> {

    private final JSOUPParser jsoupParser;
    private final ConcurrencyProperties concurrencyProperties;
    private final SitesList sites;
    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;
    private final SitePageServiceTest sitePageServiceTest;
    private final AtomicBoolean indexingIsRunning = new AtomicBoolean(false);
    private final Map<String, CompletableFuture<?>> activeIndexingTasks = new ConcurrentHashMap<>();
    private final Map<String, ForkJoinPool> activeForkJoinPools = new ConcurrentHashMap<>();

    @Override
    public Response startIndexing() {
        if (indexingIsRunning.compareAndSet(false, true)) {
            log.info("Запускаем индексацию.");
            sites.getSites().forEach(this::indexingSite);

            CompletableFuture<?>[] futures = activeIndexingTasks.values()
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures)
                    .whenCompleteAsync((res, ex) -> {
                        if (Objects.nonNull(ex)) {
                            if (ex instanceof CancellationException)
                                log.info("Индексация была отменена пользователем.");
                            else
                                log.error("Индексация завершена с ошибкой: {}.", ex.getMessage());
                        }
                        indexingIsRunning.set(false);
                    });

            return new ResponseSuccessMessageDto(true);
        }
        return new ResponseErrorMessageDto(false, "Индексация уже запущена");
    }

    private void indexingSite(Site site) {
        CompletableFuture<SiteStatus> future = CompletableFuture
                .supplyAsync(() -> prepareSiteForIndexing(site))
                .thenApply(siteService::save)
                .thenApply(savedSite -> {
                    ForkJoinPool fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                    SiteStatus result = savedSite.getSiteStatus();
                    activeForkJoinPools.put(site.getName(), fjp);
                    try {
                        LocalDateTime started = LocalDateTime.now();
                        log.info("[Time: {}] - Запущена индексация сайта {}.", started, site.getName());
                        fjp.invoke(new RecursiveSiteCrawler(savedSite,
                                site.getUrl(),
                                jsoupParser,
                                siteService,
                                pageService,
                                lemmaService,
                                indexService));
                        LocalDateTime ended = LocalDateTime.now();
                        log.info("[Time: {}] - Индексация сайта {} завершена.", ended, site.getName());
                        log.info("Длительность индексации: {} секунд.", Duration.between(started, ended).toSeconds());
                    } finally {
                        fjp.shutdownNow();
                        activeIndexingTasks.remove(site.getName());
                    }
                    return result;
                });
        activeIndexingTasks.put(site.getName(), future);
    }

    private SiteDto prepareSiteForIndexing(Site site) {
        String siteUrl = site.getUrl();

        log.info("Очищаем БД от записей по сайту {}.", siteUrl);
        sitePageServiceTest.clearDatabaseFromSitePageLemmaIndexEntities(site);

        return SiteDto.builder()
                .statusTime(LocalDateTime.now())
                .name(site.getName())
                .lastError(null)
                .url(siteUrl)
                .siteStatus(SiteStatus.INDEXING)
                .build();
    }

    @SneakyThrows
    @Override
    public Response stopIndexing() {

        log.warn("Попытка остановить индексацию.");

        if (!indexingIsRunning.get())
            return new ResponseErrorMessageDto(false, "Индексация не запущена.");

        activeIndexingTasks.values().forEach(f -> f.cancel(true));
        activeForkJoinPools.forEach((siteName, forkJoinPool) -> {
            if (!forkJoinPool.isTerminated() && !forkJoinPool.isTerminating()) {
                forkJoinPool.shutdownNow();
                try {
                    if (!forkJoinPool.awaitTermination(concurrencyProperties.getShutdownTimeout(), TimeUnit.SECONDS)) {
                        log.warn("Индексация не остановилась! Повторная попытка прерывания индексации.");
                        forkJoinPool.shutdownNow();
                        forkJoinPool.awaitTermination(concurrencyProperties.getShutdownTimeout() * 2L, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        activeForkJoinPools.clear();
        activeIndexingTasks.clear();
        System.gc();
        indexingIsRunning.set(false);

        log.info("Индексация завершилась");

        return new ResponseSuccessMessageDto(true);
    }

    public Response indexPage(String url) {
        if (indexingIsRunning.get())
            return new ResponseErrorMessageDto(false,
                    "Индексация уже запущена");

        return new ResponseSuccessMessageDto(true);
    }
}