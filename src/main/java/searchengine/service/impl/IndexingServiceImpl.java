package searchengine.service.impl;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.props.concurrency.ConcurrencyProperties;
import searchengine.exception.SiteNotIndexedException;
import searchengine.model.SiteStatus;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.demo.ResponseErrorMessageDto;
import searchengine.model.dto.response.demo.ResponseSuccessMessageDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.IndexingService;
import searchengine.service.crud.impl.IndexServiceCRUDImpl;
import searchengine.service.crud.impl.LemmaServiceCRUDImpl;
import searchengine.service.crud.impl.PageServiceCRUDImpl;
import searchengine.service.crud.impl.SiteServiceCRUDImpl;
import searchengine.service.morphology.LemmaFinder;
import searchengine.service.recursive.RecursiveSiteCrawler;
import searchengine.util.jsoup.JSOUPParser;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class IndexingServiceImpl implements IndexingService<Response> {

    private final JSOUPParser jsoupParser;
    private final ConcurrencyProperties concurrencyProperties;
    private final SitesList sites;
    private final SiteServiceCRUDImpl siteService;
    private final PageServiceCRUDImpl pageService;
    private final IndexServiceCRUDImpl indexService;
    private final LemmaServiceCRUDImpl lemmaService;
    private final LemmaFinder lemmaFinder;
    private final ExecutorService defaultIndexingExecutor;
    private final AtomicBoolean indexingIsRunning = new AtomicBoolean(false);
    private final Map<String, CompletableFuture<?>> activeIndexingTasks = new ConcurrentHashMap<>();
    private final Map<String, ForkJoinPool> activeForkJoinPools = new ConcurrentHashMap<>();

    public IndexingServiceImpl(JSOUPParser jsoupParser,
                               ConcurrencyProperties concurrencyProperties,
                               SitesList sites,
                               SiteServiceCRUDImpl siteService,
                               PageServiceCRUDImpl pageService,
                               IndexServiceCRUDImpl indexService,
                               LemmaServiceCRUDImpl lemmaService,
                               LemmaFinder lemmaFinder) {
        this.jsoupParser = jsoupParser;
        this.concurrencyProperties = concurrencyProperties;
        this.sites = sites;
        this.siteService = siteService;
        this.pageService = pageService;
        this.indexService = indexService;
        this.lemmaService = lemmaService;
        this.lemmaFinder = lemmaFinder;
        this.defaultIndexingExecutor = new ThreadPoolExecutor(
                5,
                10,
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    @Transactional
    public Response startIndexing() {
        if (indexingIsRunning.compareAndSet(false, true)) {
            log.info("Запускаем индексацию.");

            CompletableFuture[] futures = sites.getSites()
                    .stream()
                    .map(site -> oneMethodAsync(site, site.getUrl())
                                    .whenCompleteAsync((res, ex) -> {
                                        if (Objects.nonNull(ex))
                                            handleSiteIndexingError(ex, site.getUrl());
                                        else
                                            siteService.findByName(site.getName())
                                                    .ifPresent(siteDto -> siteService.updateSiteStatus(siteDto.getId(), SiteStatus.INDEXED));
                                    }, defaultIndexingExecutor))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures)
                    .whenComplete((res, ex) -> {
                        indexingIsRunning.set(false);
                        log.info("Индексация всех сайтов завершена.");
                    });

            return new ResponseSuccessMessageDto(true);
        }
        return new ResponseErrorMessageDto(false, "Индексация уже запущена");
    }

    private void handleSiteIndexingError(Throwable ex, String siteName) {
        final SiteStatus failedStatus = SiteStatus.FAILED;

        Throwable cause = ex.getCause();
        String errorDescription = Objects.nonNull(cause) ? cause.getMessage() : ex.getMessage();

        if (cause instanceof SiteNotIndexedException exception)
            log.error("Завершение индексации ошибкой: {}", exception.getMessage());
        else
            log.error("Индексация завершена с неизвестной ошибкой: {}.", ex.getCause().getClass().getSimpleName());

        siteService.findByName(siteName).ifPresentOrElse(siteDto -> {
                    siteDto.setSiteStatus(failedStatus);
                    siteDto.setLastError(errorDescription);
                    siteService.update(siteDto);
                },
                () -> log.error("Сайт не найден: {}", siteName));
    }

    @SneakyThrows
    @Transactional
    public Response indexPage(String url) {

        Optional<Site> optionalParentSite = findParentSite(url);
        if (optionalParentSite.isEmpty())
            return new ResponseErrorMessageDto(false,
                    "Данная страница находится за пределами сайтов, указанных в конфигурационном файле.");

        if (indexingIsRunning.compareAndSet(false, true)) {

            log.info("Запускаем индексацию отдельной страницы: {}", url);

            CompletableFuture.supplyAsync(() -> {
                        Site site = optionalParentSite.get();
                        SiteDto siteDto = siteService.findByName(site.getName())
                                .orElseGet(() -> siteService.save(SiteDto.builder()
                                        .name(site.getName())
                                        .url(site.getUrl())
                                        .statusTime(LocalDateTime.now())
                                        .siteStatus(SiteStatus.INDEXING)
                                        .lastError(null)
                                        .build()));

                        String rawPath = url.replaceFirst(site.getUrl(), "");
                        pageService.findByPathAndSiteId(rawPath, siteDto.getId())
                                .ifPresent(page -> pageService.deleteById(page.getId()));

                        try {
                            Connection.Response response = jsoupParser.parseResponse(url);
                            int statusCode = response.statusCode();

                            if (statusCode == 200) {
                                Document document = response.parse();

                                PageDto newPageDto = PageDto.builder()
                                        .code(statusCode)
                                        .path(rawPath)
                                        .content(document.html())
                                        .site(siteDto)
                                        .build();

                                PageDto savedPage = pageService.save(newPageDto);
                                new RecursiveSiteCrawler(siteDto, url, jsoupParser, siteService, pageService, lemmaService, indexService, lemmaFinder)
                                        .processLemmas(siteDto, savedPage);
                            }
                        } catch (Exception exception) {
                            throw new SiteNotIndexedException(siteDto, exception.getCause().getMessage());
                        }
                        return siteDto;
                    })
                    .whenComplete((res, ex) -> {
                        SiteStatus status;
                        if (Objects.isNull(ex))
                            status = SiteStatus.INDEXED;
                        else
                            status = SiteStatus.FAILED;

                        siteService.updateSiteStatus(res.getId(), status);
                        log.info("Индексация {} завершена", url);
                        indexingIsRunning.set(false);
                    });

            return new ResponseSuccessMessageDto(true);
        }

        return new ResponseErrorMessageDto(false, "Индексация уже запущена");
    }

    private CompletableFuture<SiteDto> oneMethodAsync(Site site, String url) {
        CompletableFuture<SiteDto> res1 = prepareSite(site);
        CompletableFuture<SiteDto> res2 = saveSite(res1);
        return startIndexingAsync(res2, url);
    }

    private CompletableFuture<SiteDto> prepareSite(Site site) {
        return CompletableFuture.supplyAsync(() -> prepareSiteForIndexing(site), defaultIndexingExecutor);
    }

    private CompletableFuture<SiteDto> saveSite(CompletableFuture<SiteDto> future) {
        return future.thenApply(siteService::save);
    }

    private CompletableFuture<SiteDto> startIndexingAsync(CompletableFuture<SiteDto> future, String url) {
        return future.thenApplyAsync(s -> {
            activeIndexingTasks.put(s.getName(), future);
            ForkJoinPool fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            String siteName = s.getName();
            activeForkJoinPools.put(siteName, fjp);
            try {
                LocalDateTime started = LocalDateTime.now();
                log.info("[Time: {}] - Запущена индексация сайта {}.", started, siteName);
                fjp.invoke(new RecursiveSiteCrawler(s,
                        url,
                        jsoupParser,
                        siteService,
                        pageService,
                        lemmaService,
                        indexService,
                        lemmaFinder,
                        true));
                LocalDateTime ended = LocalDateTime.now();
                log.info("[Time: {}] - Индексация сайта {} завершена.", ended, siteName);
                log.info("Длительность индексации: {} секунд.", Duration.between(started, ended).toSeconds());
            } finally {
                s.setSiteStatus(SiteStatus.INDEXED);
                fjp.shutdownNow();
                activeIndexingTasks.remove(siteName);
            }
            return s;
        }, defaultIndexingExecutor);
    }

    private SiteDto prepareSiteForIndexing(Site site) {
        String siteUrl = site.getUrl();

        log.info("Очищаем БД от записей по сайту {}.", siteUrl);
        siteService.clearDatabaseFromSitePageLemmaIndexEntities(site.getName());

        return SiteDto.builder()
                .statusTime(LocalDateTime.now())
                .name(site.getName())
                .lastError(null)
                .url(siteUrl)
                .siteStatus(SiteStatus.INDEXING)
                .build();
    }

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

        log.info("Индексация остановлена.");

        return new ResponseSuccessMessageDto(true);
    }


    private String normalizeUrl(String input) {
        try {
            String fixedInput = input.matches("^[a-zA-Z]+://.*") ? input : "http://" + input;
            URI uri = URI.create(fixedInput);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : input.toLowerCase();
        } catch (Exception exception) {
            return input.toLowerCase();
        }
    }

    private Optional<Site> findParentSite(String url) {
        try {
            String normalizedUrl = normalizeUrl(url);
            return sites.getSites()
                    .stream()
                    .filter(site -> normalizedUrl.equals(normalizeUrl(site.getUrl())))
                    .findFirst();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}