package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import searchengine.exception.SiteNotIndexedException;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.crud.impl.PageServiceCRUDImpl;
import searchengine.service.crud.impl.SiteServiceCRUDImpl;
import searchengine.service.morphology.LemmaProcessor;
import searchengine.util.jsoup.JSOUPParser;

import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
public class RecursiveSiteCrawler extends RecursiveAction {

    private final SiteDto siteDto;
    private final String url;
    private final JSOUPParser jsoupParser;
    private final SiteServiceCRUDImpl siteService;
    private final PageServiceCRUDImpl pageService;
    private final LemmaProcessor lemmaProcessor;
    @Setter
    private static volatile boolean cancelRecursiveTask = false;
    @Getter
    @Setter
    private final boolean newIndexing;
    private static final Set<String> parsedPages = ConcurrentHashMap.newKeySet();
    private static final Map<String, ReentrantLock> PAGE_LOCKS = new ConcurrentHashMap<>();

    public RecursiveSiteCrawler(SiteDto siteDto,
                                String url,
                                JSOUPParser jsoupParser,
                                SiteServiceCRUDImpl siteService,
                                PageServiceCRUDImpl pageService,
                                LemmaProcessor lemmaProcessor) {
        this(siteDto, url, jsoupParser, siteService, pageService, lemmaProcessor, false);
    }

    public RecursiveSiteCrawler(SiteDto siteDto,
                                String url,
                                JSOUPParser jsoupParser,
                                SiteServiceCRUDImpl siteService,
                                PageServiceCRUDImpl pageService,
                                LemmaProcessor lemmaProcessor,
                                boolean newIndexing) {
        this.siteDto = siteDto;
        this.url = url;
        this.jsoupParser = jsoupParser;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaProcessor = lemmaProcessor;
        this.newIndexing = newIndexing;

        if (newIndexing)
            parsedPages.removeIf(elem -> elem.contains(siteDto.getUrl()));
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
    protected void compute() {
        if (Thread.currentThread().isInterrupted() || cancelRecursiveTask) {
            SiteDto site = SiteDto.builder().build();
            site.setName(siteDto.getName());
            site.setUrl(siteDto.getUrl());
            throw new SiteNotIndexedException(site, "Индексация отменена пользователем");
        }

        try {
            Connection.Response response = jsoupParser.parseResponse(this.url);
            int statusCode = response.statusCode();

            Document document = response.parse();
            Set<String> pages = findChildPages(document);
            String rawPath = this.url.replaceFirst(siteDto.getUrl(), "");

            ReentrantLock pageLock = PAGE_LOCKS.computeIfAbsent(rawPath, k -> new ReentrantLock());
            pageLock.lockInterruptibly();
            try {
                PageDto pageDto = pageService.findByPathAndSiteId(rawPath, siteDto.getId())
                        .orElseGet(() -> PageDto.builder()
                                .site(siteDto)
                                .content(document.html())
                                .path(rawPath)
                                .code(statusCode).build());

                PageDto savedPage = pageService.save(pageDto);
                lemmaProcessor.processLemmas(siteDto, savedPage);

                RecursiveSiteCrawler[] tasksList = new RecursiveSiteCrawler[pages.size()];
                int p = 0;
                for (String page : pages) {
                    if (Thread.interrupted())
                        throw new InterruptedException();

                    tasksList[p++] = new RecursiveSiteCrawler(
                            siteDto,
                            page,
                            jsoupParser,
                            siteService,
                            pageService,
                            lemmaProcessor);
                }
                ForkJoinTask.invokeAll(tasksList);
            } finally {
                pageLock.unlock();
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Индексация была отменена пользователем");
        } catch (SocketTimeoutException socketTimeoutException) {
            errorLogger(socketTimeoutException, this.url);
            errorSaving(RequestStatusCode.REQUEST_TIMEOUT);
        } catch (HttpStatusException httpStatusException) {
            errorLogger(httpStatusException, httpStatusException.getUrl());
            errorSaving(RequestStatusCode.NOT_FOUND);
        } catch (CancellationException cancellationException) {
            SiteDto site = SiteDto.builder().build();
            site.setName(siteDto.getName());
            site.setUrl(siteDto.getUrl());
            throw new SiteNotIndexedException(site, "Индексация отменена пользователем");
        } catch (Exception exception) {
            errorLogger(exception, this.url);
            errorSaving(RequestStatusCode.REQUEST_DENIED);
        }
    }
    private <T extends Exception> void errorLogger(T exception, String url) {
        log.error("Возникло исключение {} при обработке страницы: {}", exception.getClass().getSimpleName(), url);
        log.error("Текст ошибки: {}", exception.getMessage());
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
                    .orElseGet(() -> pageService.save(errorDto));
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