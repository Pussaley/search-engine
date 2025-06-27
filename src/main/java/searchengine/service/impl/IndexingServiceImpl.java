package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.props.concurrency.ConcurrencyProperties;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.demo.ResponseErrorMessageDto;
import searchengine.model.dto.response.demo.ResponseSuccessMessageDto;
import searchengine.service.IndexingService;
import searchengine.service.impl.demo.TaskDemo;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.service.recursive.JsoupSiteIndexingResponse;
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
    private static ForkJoinPool forkJoinPool;
    private static ExecutorService executorService;
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
                                try {
                                    siteService.clearDatabaseBySiteName(site.getName());
                                    SiteDto savedSite = siteService.save(site);

                                    TaskDemo taskDemo = new TaskDemo(savedSite,
                                            site.getUrl(),
                                            jsoupParser,
                                            siteService,
                                            pageService,
                                            lemmaService,
                                            indexService);

                                    log.info("[Time: {}] - Запущена индексация сайта {}.", LocalDateTime.now(), site.getName());
                                    Boolean indexingResult = forkJoinPool.invoke(taskDemo);
                                    log.info("Индексация сайта {} завершена.", site.getName());

                                    SiteStatus status = indexingResult ? SiteStatus.INDEXED : SiteStatus.FAILED;

                                    savedSite.setSiteStatus(status);
                                    siteService.updateSite(savedSite);

                                    isRunning.set(false);
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

        try {
            Objects.requireNonNull(forkJoinPool);

            if (!isRunning.get()) {
                log.info("Индексация не запущена");
                return new ResponseErrorMessageDto(isRunning.get(), "Индексация не запущена");
            }

            int defaultTimeOut = concurrencyProperties.getShutdownTimeout();
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
        } catch (NullPointerException npe) {
            return new ResponseErrorMessageDto(isRunning.get(), "Индексация не запущена, FJP не был инициализирован");
        }
    }

    public Response indexPage(String url) {
        try {
            if (isRunning.get())
                return new ResponseErrorMessageDto(false,
                        "Индексация уже запущена");
            if (!isRelativePage(url))
                return new ResponseErrorMessageDto(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
            else {
                initFJP();
                initExecutorService(Executors.newCachedThreadPool());
                isRunning.set(true);
                executorService.submit(() -> {

                    URI uri = URI.create(url);
                    String pageScheme = uri.getScheme();
                    String pageHost = uri.getHost();
                    String pagePath = uri.getPath();
                    String pageRawPath = uri.getPath().concat(Objects.isNull(uri.getQuery()) ? "" : url.split(uri.getPath())[1]);

                    String siteUrl = pageScheme.concat("://").concat(pageHost);

                    for (Site site : sites.getSites()) {
                        if (site.getUrl().contains(siteUrl)) {
                            log.info("Cайт {} найден в БД, значит ", siteUrl);
                            siteService.save(site);
                            break;
                        }
                    }

                    ForkJoinRecursiveTask recursiveTask = new ForkJoinRecursiveTask(url);
                    Response indexingResult = forkJoinPool.invoke(recursiveTask);
                });
                return new ResponseSuccessMessageDto(isRunning.get());
            }
        } catch (Exception exception) {
            log.info(exception.getClass().getSimpleName());
            return new ResponseErrorMessageDto(false,
                    MessageFormat.format("Неизвестная ошибка: {0}. Message: {1}",
                            exception.getClass().getSimpleName(),
                            exception.getMessage()));
        }
    }

    private boolean isRelativePage(String url) throws Exception {
        URI uri = URI.create(url);
        return sites.getSites().stream().anyMatch(site -> site.getUrl().contains(uri.getScheme().concat("://").concat(uri.getHost())));
    }

    private static ForkJoinPool initFJP() {
        if (Objects.isNull(forkJoinPool))
            forkJoinPool = new ForkJoinPool();
        return forkJoinPool;
    }

    private static ExecutorService initExecutorService(int size) {
        return initExecutorService(Executors.newFixedThreadPool(size));
    }

    private static ExecutorService initExecutorService(ExecutorService executor) {
        if (Objects.isNull(executorService))
            executorService = executor;
        return executorService;
    }
}