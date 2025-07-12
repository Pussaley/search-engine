package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
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

import java.net.URI;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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

    private final JSOUPParser jsoupParser;
    private final ConcurrencyProperties concurrencyProperties;
    private final SitesList sites;
    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;
    private final SitePageServiceTest sitePageServiceTest;
    private ForkJoinPool forkJoinPool;
    private ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public Response startIndexing() {

        if (isRunning.compareAndSet(false, true)) {
            log.info("Запускаем индексацию.");
            List<Site> siteList = sites.getSites();
            initFJP();
            initExecutorService(siteList.size());

            executorService.submit(() -> siteList.forEach(site -> executorService.submit(
                            () -> {
                                SiteDto savedSite = null;
                                SiteDto siteDto = SiteDto.builder()
                                        .siteStatus(SiteStatus.INDEXING)
                                        .statusTime(LocalDateTime.now())
                                        .name(site.getName())
                                        .url(site.getUrl())
                                        .lastError(null)
                                        .build();

                                try {
                                    sitePageServiceTest.clearDatabaseFromSitePageLemmaIndexEntities(site);
                                    log.info("Очищение базы успешно завершено");

                                    savedSite = siteService.save(siteDto);

                                    log.info("[Time: {}] - Запущена индексация сайта {}.", LocalDateTime.now(), site.getName());
                                    SiteStatus status = crawlingSite(new RecursiveSiteCrawler(savedSite,
                                            site.getUrl(),
                                            jsoupParser,
                                            siteService,
                                            pageService,
                                            lemmaService,
                                            indexService,
                                            sitePageServiceTest,
                                            true));
                                    log.info("Индексация сайта {} завершена.", site.getName());

                                    savedSite.setSiteStatus(status);
                                } catch (Exception exception) {
                                    if (Objects.nonNull(savedSite)) {
                                        savedSite.setLastError(exception.getMessage());
                                        savedSite.setSiteStatus(SiteStatus.FAILED);
                                    }
                                    log.error("Исключение {} во время индексации сайта {}", exception.getClass().getSimpleName(), site.getName());
                                    exception.printStackTrace();
                                } finally {
                                    if (Objects.nonNull(savedSite)) {
                                        savedSite.setStatusTime(LocalDateTime.now());
                                        siteService.update(savedSite);
                                    }
                                    isRunning.set(false);
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

    public Response indexPage(String url) {
        if (isRunning.get())
            return new ResponseErrorMessageDto(false,
                    "Индексация уже запущена");
        if (!isRelativePage(url))
            return new ResponseErrorMessageDto(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");

        try {
            initFJP();
            initExecutorService(1);
            isRunning.set(true);
            executorService.submit(() -> {
                try {
                    for (Site site : sites.getSites()) {
                        if (url.contains(site.getUrl())) {
                            sitePageServiceTest.clearDatabaseFromSitePageLemmaIndexEntities(site);
                            log.info("Очищение базы успешно завершено");
                            SiteDto siteDto = SiteDto.builder()
                                    .siteStatus(SiteStatus.INDEXING)
                                    .statusTime(LocalDateTime.now())
                                    .name(site.getName())
                                    .url(site.getUrl())
                                    .lastError("")
                                    .build();

                            SiteDto savedSiteDto = siteService.save(siteDto);
                            log.info("[Time: {}] - Запущена индексация сайта {}.", LocalDateTime.now(), savedSiteDto.getName());
                            SiteStatus status = crawlingSite(new RecursiveSiteCrawler(
                                    savedSiteDto,
                                    url,
                                    jsoupParser,
                                    siteService,
                                    pageService,
                                    lemmaService,
                                    indexService,
                                    sitePageServiceTest));
                            log.info("Индексация сайта {} завершена.", site.getName());

                            savedSiteDto.setSiteStatus(status);
                            siteService.update(savedSiteDto);
                        }
                    }
                } finally {
                    isRunning.set(false);
                }
            });
        } catch (Exception exception) {
            log.info(exception.getClass().getSimpleName());
            return new ResponseErrorMessageDto(false,
                    MessageFormat.format("Неизвестная ошибка: {0}. Message: {1}",
                            exception.getClass().getSimpleName(),
                            exception.getMessage()));
        }
        return new ResponseSuccessMessageDto(isRunning.get());
    }

    private SiteStatus crawlingSite(RecursiveSiteCrawler siteCrawler) {
        Boolean indexingResult = forkJoinPool.invoke(siteCrawler);
        return indexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED;
    }

    @Override
    public Response stopIndexing() {
        if (!isRunning.get()) {
            log.info("Индексация не запущена");
            return new ResponseErrorMessageDto(isRunning.get(), "Индексация не запущена");
        }

        executorService.submit(() -> {
            int defaultTimeOut = concurrencyProperties.getShutdownTimeout();

            boolean terminated = false;
            try {
                forkJoinPool.shutdownNow();
                log.info("Индексация была остановлена пользователем");
                terminated = forkJoinPool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.info("Ожидание завершения работы ForkJoinPool'а было прервано <{}>", e.getMessage());
                Thread.currentThread().interrupt();
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

                siteService.updateAllSitesSiteStatus(SiteStatus.INDEXING, SiteStatus.FAILED);
                isRunning.set(false);
                log.info("Индексация полностью остановлена");
            }
        });
        return new ResponseSuccessMessageDto(true);
    }

    private boolean isRelativePage(String url) {
        URI uri = URI.create(url);
        return sites.getSites().stream().anyMatch(site -> site.getUrl().contains(uri.getScheme().concat("://").concat(uri.getHost())));
    }

    private void initFJP() {
        if (forkJoinPool == null || forkJoinPool.isShutdown()) {
            forkJoinPool = new ForkJoinPool();
        }
    }

    private void initExecutorService(int size) {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(size);
        }
    }
}