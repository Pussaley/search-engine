package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.exception.IndexingCancelledByUserException;
import searchengine.model.SiteStatus;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.crud.impl.PageServiceCRUDImpl;
import searchengine.service.crud.impl.SiteServiceCRUDImpl;
import searchengine.service.morphology.LemmaProcessor;
import searchengine.util.jsoup.JSOUPParser;
import searchengine.util.url.UrlNormalizer;

import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final UrlNormalizer urlNormalizer;
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
                                LemmaProcessor lemmaProcessor, UrlNormalizer urlNormalizer) {
        this(siteDto, url, jsoupParser, siteService, pageService, lemmaProcessor, urlNormalizer, false);
    }

    public RecursiveSiteCrawler(SiteDto siteDto,
                                String url,
                                JSOUPParser jsoupParser,
                                SiteServiceCRUDImpl siteService,
                                PageServiceCRUDImpl pageService,
                                LemmaProcessor lemmaProcessor,
                                UrlNormalizer urlNormalizer,
                                boolean newIndexing) {
        this.siteDto = siteDto;
        this.url = url;
        this.jsoupParser = jsoupParser;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaProcessor = lemmaProcessor;
        this.urlNormalizer = urlNormalizer;
        this.newIndexing = newIndexing;

        if (newIndexing)
            parsedPages.removeIf(elem -> elem.contains(siteDto.getUrl()));

        cancelRecursiveTask = false;
    }

    private Set<String> findChildPages(Document document) {
        String formats = "yml|yaml|nc|eps|ws|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
        String cssSelector = "a:not([href~=(#|tel|mailto)|(?i)\\.(".concat(formats).concat(")])");

        Set<String> pages = findPagination(document)
                .stream()
                .filter(parsedPages::add)
                .collect(Collectors.toSet());

        Set<String> strings = document.select(cssSelector).stream()
                .map(e -> e.attr("abs:href"))
                .filter(e -> e.startsWith(siteDto.getUrl()))
                .map(urlNormalizer::appendTrailingSlashIfNeeded)
                .filter(parsedPages::add)
                .collect(Collectors.toCollection(HashSet::new));

        strings.addAll(pages);

        return strings;
    }

    private Set<String> findPagination(Document document) {
        return document.baseUri().contains("playback.ru")
                ? toPageableUrls(document)
                : Collections.emptySet();
    }

    private Set<String> toPageableUrls(Document document) {
        final String baseUri = document.baseUri();

        Elements elements = document.select("div.pager span.page");
        if (elements.isEmpty())
            return Collections.emptySet();

        String uri = (baseUri.contains("page="))
                ? baseUri.replaceFirst("page=\\d+", "page=")
                : baseUri.endsWith("/")
                ? baseUri.concat("page=")
                : baseUri.concat("/page=");

        return elements.stream()
                .map(Element::text)
                .map(uri::concat)
                .collect(Collectors.toSet());
    }

    @Override
    protected void compute() {
        if (Thread.currentThread().isInterrupted() || cancelRecursiveTask) {
            final String error = "Индексация отменена пользователем";
            throw new IndexingCancelledByUserException(createCanceledSiteDtoWithStatus(SiteStatus.FAILED, error), error);
        }

        try {
            Connection.Response response = jsoupParser.parseResponse(this.url);
            int statusCode = response.statusCode();

            Document document = response.parse();
            Set<String> pages = findChildPages(document);
            String rawPath = this.url.replaceFirst(siteDto.getUrl(), "");
            final String finalRawPath = rawPath.isEmpty() ? "/" : rawPath;

            ReentrantLock pageLock = PAGE_LOCKS.computeIfAbsent(finalRawPath, k -> new ReentrantLock());
            List<RecursiveSiteCrawler> tasksList = new ArrayList<>();

            try {
                pageLock.lockInterruptibly();
                if (pageService.findByPathAndSiteId(finalRawPath, siteDto.getId()).isEmpty()) {
                    PageDto pageDto = PageDto.builder()
                            .site(siteDto)
                            .content(document.html())
                            .path(finalRawPath)
                            .code(statusCode).build();

                    PageDto savedPage = pageService.save(pageDto);
                    lemmaProcessor.processLemmas(siteDto, savedPage);
                }

                for (String page : pages) {
                    if (Thread.interrupted())
                        throw new InterruptedException();

                    tasksList.add(new RecursiveSiteCrawler(
                            siteDto,
                            page,
                            jsoupParser,
                            siteService,
                            pageService,
                            lemmaProcessor,
                            urlNormalizer));
                }
            } finally {
                pageLock.unlock();
                if (!tasksList.isEmpty()) {
                    tasksList.removeIf(Objects::isNull);
                    ForkJoinTask.invokeAll(tasksList);
                }
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
        } catch (IndexingCancelledByUserException indexingCancelledByUserException) {
            cancelRecursiveTask = true;
            throw indexingCancelledByUserException;
        } catch (CancellationException | IllegalMonitorStateException cancelException) {
            final String error = "Индексация отменена пользователем";
            throw new IndexingCancelledByUserException(createCanceledSiteDtoWithStatus(SiteStatus.FAILED, error), error);
        } catch (Exception exception) {
            errorLogger(exception, this.url);
            errorSaving(RequestStatusCode.REQUEST_DENIED);
        }
    }

    private SiteDto createCanceledSiteDtoWithStatus(SiteStatus status, String error) {
        return SiteDto.builder()
                .statusTime(LocalDateTime.now())
                .siteStatus(status)
                .lastError(error)
                .name(this.siteDto.getName())
                .url(this.siteDto.getUrl())
                .build();
    }

    private <T extends Exception> void errorLogger(T exception, String url) {
        if (cancelRecursiveTask)
            return;
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