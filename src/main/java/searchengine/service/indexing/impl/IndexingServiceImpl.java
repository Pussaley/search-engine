package searchengine.service.indexing.impl;

import lombok.Getter;
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
import searchengine.model.dto.response.indexing.IndexingResultResponse;
import searchengine.model.dto.response.indexing.ResponseErrorMessageDto;
import searchengine.model.dto.response.indexing.ResponseSuccessMessageDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.crud.impl.PageServiceCRUDImpl;
import searchengine.service.crud.impl.SiteServiceCRUDImpl;
import searchengine.service.indexing.IndexingService;
import searchengine.service.morphology.LemmaProcessor;
import searchengine.service.recursive.RecursiveSiteCrawler;
import searchengine.util.jsoup.JSOUPParser;
import searchengine.util.url.UrlNormalizer;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    private final UrlNormalizer urlNormalizer;
    private final ConcurrencyProperties concurrencyProperties;
    private final SitesList sites;
    private final SiteServiceCRUDImpl siteService;
    private final PageServiceCRUDImpl pageService;
    private final LemmaProcessor lemmaProcessor;
    private final ExecutorService defaultIndexingExecutor;
    @Getter
    private final AtomicBoolean indexingIsRunning = new AtomicBoolean(false);
    private final Map<String, CompletableFuture<?>> activeIndexingTasks = new ConcurrentHashMap<>();
    private final Map<String, ForkJoinPool> activeForkJoinPools = new ConcurrentHashMap<>();

    public IndexingServiceImpl(JSOUPParser jsoupParser,
                               UrlNormalizer urlNormalizer,
                               ConcurrencyProperties concurrencyProperties,
                               SitesList sites,
                               SiteServiceCRUDImpl siteService,
                               PageServiceCRUDImpl pageService,
                               LemmaProcessor lemmaProcessor) {
        this.jsoupParser = jsoupParser;
        this.urlNormalizer = urlNormalizer;
        this.concurrencyProperties = concurrencyProperties;
        this.sites = sites;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaProcessor = lemmaProcessor;
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
                    .map(site -> globalIndexing(site, site.getUrl())
                            .whenCompleteAsync((res, ex) -> {
                                String siteName = site.getName();

                                if (Objects.nonNull(ex))
                                    handleSiteIndexingError(ex, siteName);
                                else
                                    siteService.findByName(siteName)
                                            .ifPresent(siteDto -> siteService.updateSiteStatus(siteDto.getId(), SiteStatus.INDEXED));
                            }, defaultIndexingExecutor))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures)
                    .whenComplete((res, ex) -> {
                        clearResources();
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

    public Response indexPageAsync(String url) {

        Optional<Site> optionalParentSite = findParentSite(url);
        if (optionalParentSite.isEmpty())
            return new ResponseErrorMessageDto(false,
                    "Данная страница находится за пределами сайтов, указанных в конфигурационном файле.");

        if (indexingIsRunning.compareAndSet(false, true)) {

            log.info("Запускаем индексацию отдельной страницы: {}", url);

            Site site = optionalParentSite.get();
            CompletableFuture<IndexingResultResponse> completedPageIndexing = CompletableFuture.supplyAsync(() -> preparePage(site, url))
                    .thenApply(siteDto -> indexPageAsync(siteDto, url))
                    .whenComplete((result, throwable) -> handlePageIndexingResult(site, result, throwable));

            CompletableFuture.allOf(completedPageIndexing)
                    .thenRunAsync(() -> log.info("Индексация страницы {} завершена", url), defaultIndexingExecutor);

            return new ResponseSuccessMessageDto(true);
        }

        return new ResponseErrorMessageDto(false, "Индексация уже запущена");
    }

    private void handlePageIndexingResult(Site site, IndexingResultResponse result, Throwable throwable) {
        try {
            Long siteId = siteService.findByName(site.getName()).map(SiteDto::getId).orElseThrow();

            if (throwable != null)
                siteService.updateSiteStatusWithError(siteId, SiteStatus.FAILED, throwable.getCause().getMessage());
            else
                siteService.updateSiteStatus(siteId, SiteStatus.INDEXED);
        } finally {
            indexingIsRunning.set(false);
        }
    }

    private CompletableFuture<SiteDto> globalIndexing(Site site, String url) {
        SiteDto savedSite = siteService.save(prepareSiteForIndexing(site));
        return CompletableFuture.supplyAsync(() -> startIndexingAsync(savedSite, url), defaultIndexingExecutor);
    }

    private SiteDto preparePage(Site site, String url) {
        SiteDto siteDto = siteService.findByName(site.getName())
                .orElseGet(() -> siteService.save(SiteDto.builder()
                        .name(site.getName())
                        .url(site.getUrl())
                        .statusTime(LocalDateTime.now())
                        .siteStatus(SiteStatus.INDEXING)
                        .lastError(null)
                        .build()));

        if (siteDto.getSiteStatus() != SiteStatus.INDEXING)
            siteService.updateSiteStatus(siteDto.getId(), SiteStatus.INDEXING);

        String rawPath = url.replaceFirst(site.getUrl(), "");
        pageService.findByPathAndSiteId(rawPath, siteDto.getId())
                .ifPresent(pageService::delete);

        return siteDto;
    }

    private IndexingResultResponse indexPageAsync(SiteDto siteDto, String url) {
        try {
            Connection.Response response = jsoupParser.parseResponse(url);
            int statusCode = response.statusCode();
            String rawPath = url.replaceFirst(siteDto.getUrl(), "");
            Document document = response.parse();

            pageService.findByPathAndSiteId(rawPath, siteDto.getId())
                    .ifPresentOrElse(page -> {
                        throw new RuntimeException(MessageFormat.format("Страница {0} уже существует в Базе", page));
                    }, () -> {
                        PageDto savedPage = pageService.save(PageDto.builder()
                                .site(siteDto)
                                .content(document.html())
                                .path(rawPath)
                                .code(statusCode).build());

                        lemmaProcessor.processLemmas(siteDto, savedPage);
                    });

            return new IndexingResultResponse(siteDto, SiteStatus.INDEXED, null);
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private SiteDto startIndexingAsync(SiteDto s, String url) {

        CompletableFuture<SiteDto> future = new CompletableFuture<>();
        activeIndexingTasks.put(s.getName(), future);
        ForkJoinPool fjp = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        String siteName = s.getName();
        activeForkJoinPools.put(siteName, fjp);
        try {
            LocalDateTime started = LocalDateTime.now();
            log.info("[Time: {}] - Запущена индексация сайта {}.", started, siteName);
            fjp.invoke(new RecursiveSiteCrawler(s, url, jsoupParser, siteService, pageService, lemmaProcessor, urlNormalizer, true));
            LocalDateTime ended = LocalDateTime.now();
            log.info("[Time: {}] - Индексация сайта {} завершена.", ended, siteName);
            log.info("Длительность индексации: {} секунд.", Duration.between(started, ended).toSeconds());
        } finally {
            s.setSiteStatus(SiteStatus.INDEXED);
            fjp.shutdownNow();
            activeIndexingTasks.remove(siteName);
        }
        return s;
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

        clearResources();
        indexingIsRunning.set(false);

        log.info("Индексация остановлена.");

        return new ResponseSuccessMessageDto(true);
    }

    private Optional<Site> findParentSite(String url) {
        try {
            String normalizedUrl = urlNormalizer.normalize(url);
            return sites.getSites()
                    .stream()
                    .filter(site -> normalizedUrl.equals(urlNormalizer.normalize(site.getUrl())))
                    .findFirst();
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
    private void clearResources() {
        activeForkJoinPools.clear();
        activeIndexingTasks.clear();
        System.gc();
    }
}