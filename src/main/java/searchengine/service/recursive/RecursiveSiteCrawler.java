package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.demo.SitePageServiceTest;
import searchengine.service.impl.IndexServiceImpl;
import searchengine.service.impl.LemmaServiceImpl;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.morphology.LemmaFinder;
import searchengine.util.jsoup.JSOUPParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class RecursiveSiteCrawler extends RecursiveTask<Boolean> {

    private final SiteDto siteDto;
    private final String url;
    private final JSOUPParser jsoupParser;
    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;
    private final SitePageServiceTest pageServiceTest;
    @Getter
    @Setter
    private static volatile boolean cancelRecursiveTask = false;
    @Getter
    @Setter
    private final boolean newIndexing;
    private static final Set<String> parsedPages = ConcurrentHashMap.newKeySet();
    private static final Map<String, ReentrantLock> PAGE_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> LEMMA_LOCKS = new ConcurrentHashMap<>();

    public RecursiveSiteCrawler(SiteDto siteDto, String url, JSOUPParser jsoupParser, SiteServiceImpl siteService, PageServiceImpl pageService, LemmaServiceImpl lemmaService, IndexServiceImpl indexService, SitePageServiceTest pageServiceTest) {
        this(siteDto, url, jsoupParser, siteService, pageService, lemmaService, indexService, pageServiceTest, true);
    }

    public RecursiveSiteCrawler(SiteDto siteDto,
                                String url,
                                JSOUPParser jsoupParser,
                                SiteServiceImpl siteService,
                                PageServiceImpl pageService,
                                LemmaServiceImpl lemmaService,
                                IndexServiceImpl indexService,
                                SitePageServiceTest pageServiceTest,
                                boolean newIndexing) {
        this.siteDto = siteDto;
        this.url = url;
        this.jsoupParser = jsoupParser;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.pageServiceTest = pageServiceTest;
        this.newIndexing = newIndexing;

        if (newIndexing)
            parsedPages.removeIf(elem -> elem.contains(siteDto.getUrl()));
    }

    private boolean isPositive(int statusCode) {
        return statusCode < 400;
    }

    private Set<String> findChildPages(Document document) {
        String formats = "yml|yaml|nc|eps|ws|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
        String cssSelector = "a:not([href~=(#|tel|mailto)|(?i)\\.(".concat(formats).concat(")])");

        return document.select(cssSelector).stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> e.startsWith(siteDto.getUrl()))
                .map(e -> e.endsWith("/") ? e : e.concat("/"))
                .filter(parsedPages::add)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    protected Boolean compute() {

        if (cancelRecursiveTask) {
            cancel(true);
            return false;
        }

        try {
            Connection.Response response = jsoupParser.parseResponse(this.url);
            int statusCode = response.statusCode();

            if (!isPositive(statusCode))
                return false;

            Document document = response.parse();

            Set<String> pages = findChildPages(document);
            ForkJoinTask<Boolean>[] tasksArray = new ForkJoinTask[pages.size()];

            int pagePosition = 0;
            for (String page : pages) {
                String rawPath = page.replaceFirst(siteDto.getUrl(), "");
                ReentrantLock pageLock = PAGE_LOCKS.computeIfAbsent(rawPath, k -> new ReentrantLock());
                pageLock.lock();
                try {
                    Optional<PageDto> pageOptional = pageService.findByPathAndSiteId(rawPath, siteDto.getId());
                    if (pageOptional.isPresent())
                        continue;

                    processLemmas(pageServiceTest.savePage(PageDto.builder()
                            .site(siteDto)
                            .content(document.html())
                            .path(rawPath)
                            .code(statusCode).build()));
                } finally {
                    pageLock.unlock();
                }

                tasksArray[pagePosition++] = new RecursiveSiteCrawler(
                        siteDto,
                        page,
                        jsoupParser,
                        siteService,
                        pageService,
                        lemmaService,
                        indexService,
                        pageServiceTest,
                        false).fork();
            }
            ForkJoinTask.invokeAll(tasksArray);
        } catch (SocketTimeoutException socketTimeoutException) {
            errorLogger(socketTimeoutException, this.url);
            errorSaving(RequestStatusCode.REQUEST_TIMEOUT);
        } catch (HttpStatusException httpStatusException) {
            errorLogger(httpStatusException, httpStatusException.getUrl());
            errorSaving(RequestStatusCode.NOT_FOUND);
        } catch (Exception exception) {
            errorLogger(exception, this.url);
            errorSaving(RequestStatusCode.REQUEST_DENIED);
        }

        return true;
    }

    private void processLemmas(PageDto page) {
        LemmaFinder lemmaFinder;
        try {
            lemmaFinder = LemmaFinder.getInstance();
        } catch (IOException e) {
            log.error("Ошибка при создании лемматизатора");
            return;
        }
        ReentrantLock pageLock = PAGE_LOCKS.computeIfAbsent(page.getPath(), k -> new ReentrantLock());
        pageLock.lock();
        try {
            Map<String, Integer> lemmas = lemmaFinder.collectLemmas(page.getContent());
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                String lemma = entry.getKey();
                Integer lemmaCount = entry.getValue();

                ReentrantLock lemmaLock = LEMMA_LOCKS.computeIfAbsent(lemma, k -> new ReentrantLock());
                lemmaLock.lock();
                try {
                    LemmaDto lemmaDto = lemmaService.findByLemmaAndSiteId(lemma, siteDto.getId())
                            .orElseGet(() -> LemmaDto.builder()
                                    .lemma(lemma)
                                    .site(siteDto)
                                    .frequency(0)
                                    .build());

                    lemmaDto.setFrequency(lemmaDto.getFrequency() + 1);
                    LemmaDto savedLemma = lemmaService.save(lemmaDto);

                    indexService.findByPageAndLemma(page, savedLemma).orElseGet(() ->
                            indexService.save(IndexDto.builder()
                                    .pageId(page.getId())
                                    .lemmaId(savedLemma.getId())
                                    .rank(lemmaCount.floatValue())
                                    .build()));
                } finally {
                    lemmaLock.unlock();
                }
            }
        } finally {
            pageLock.unlock();
        }
    }

    private <T extends Exception> void errorLogger(T exception, String url) {
        log.error("Возникло исключение {} при обработке страницы: {}", exception.getClass().getSimpleName(), url);
    }

    private void errorSaving(RequestStatusCode statusCode) {
        String rawPath = this.url.replaceFirst(siteDto.getUrl(), "");
        ReentrantLock pageLock = PAGE_LOCKS.computeIfAbsent(rawPath, k -> new ReentrantLock());

        pageLock.lock();
        try {
            PageDto errorDto = PageDto.builder()
                    .site(siteDto)
                    .content("")
                    .path(rawPath)
                    .code(statusCode.getCode())
                    .build();
            pageService.findByPathAndSiteId(rawPath, siteDto.getId())
                    .orElseGet(() -> {
                        log.info("Сохраняем {}", errorDto);
                        return pageServiceTest.savePage(errorDto);
                    });
        } finally {
            pageLock.unlock();
        }
    }

    @Getter
    private enum RecursiveTaskError {
        MAIN_PAGE_REJECTED("Главная страница сайта не отвечает"),
        STOPPED_BY_USER("Индексация остановлена пользователем"),
        UNEXPECTED_ERROR("Неизвестная ошибка");

        private final String description;

        RecursiveTaskError(String description) {
            this.description = description;
        }
    }

    @Getter
    private enum RequestStatusCode {
        NOT_FOUND(404),
        REQUEST_TIMEOUT(408),
        REQUEST_DENIED(500);

        private final int code;

        RequestStatusCode(int code) {
            this.code = code;
        }
    }
}