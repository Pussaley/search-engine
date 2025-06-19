package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.exception.SiteNotFoundException;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.util.jsoup.JSOUPParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class ForkJoinRecursiveTask extends RecursiveTask<JsoupSiteIndexingResponse> {

    private final JSOUPParser jsoupParser = SearchEngineApplicationContext.getBean(JSOUPParser.class);
    private final PageServiceImpl pageService = SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private final SiteServiceImpl siteService = SearchEngineApplicationContext.getBean(SiteServiceImpl.class);
    private final String url;
    private static final Set<String> parsedURLs = new CopyOnWriteArraySet<>();
    @Getter
    @Setter
    private volatile boolean cancel = false;

    private ForkJoinRecursiveTask(String url) {
        this.url = url;
    }

    public ForkJoinRecursiveTask(Site site) {
        this(site.getUrl());
    }

    @Override
    protected JsoupSiteIndexingResponse compute() {
        if (cancel) {
            this.cancel(true);
            return JsoupSiteIndexingResponse.failure(RecursiveTaskError.STOPPED_BY_USER.getDescription());
        }

        try {
            List<ForkJoinTask<JsoupSiteIndexingResponse>> tasks = new ArrayList<>();

            parsedURLs.add(this.url);
            log.info("Добавили {} в parsedURLs[{}]", this.url, parsedURLs.size());

            Connection.Response response = jsoupParser.parse(this.url);

            String rootUrl = Objects.isNull(response) ? new URL(this.url).getPath() : response.url().getPath();
            if (rootUrl.equalsIgnoreCase("/") && Objects.isNull(response)) {
                log.error("Главная страница сайта {} недоступна, завершаем индексацию", this.url);
                return JsoupSiteIndexingResponse.failure(RecursiveTaskError.MAIN_PAGE_REJECTED.getDescription());
            }
            Objects.requireNonNull(response);
            Collection<String> nodes = jsoupParser.parseAbsLinks(this.url);

            PageDto pageDto = pageService.createDtoFromJsoupResponse(response);
            String pagePath = pageDto.getPath();
            String siteUrl = pageDto.getSite().getUrl();

            Optional<PageDto> foundPageDtoOptional;
            try {
                foundPageDtoOptional = pageService.findByPath(pagePath);
            } catch (IncorrectResultSizeDataAccessException incorrectResultSizeDataAccessException) {
                foundPageDtoOptional = pageService.findByPathAndSiteUrl(pagePath, siteUrl);
            }
            foundPageDtoOptional.ifPresentOrElse((foundDto) -> {
                if (!foundDto.getSite().getUrl().equalsIgnoreCase(siteUrl))
                    pageService.save(pageDto);
            }, () -> pageService.save(pageDto));

            if (!nodes.isEmpty()) {
                nodes.removeIf(parsedURLs::contains);
                nodes.stream()
                        .peek((link) -> log.info("Создаем ForkJoinRecursiveTask с ссылкой {}", link))
                        .map(ForkJoinRecursiveTask::new)
                        .forEachOrdered(tasks::add);

                ForkJoinTask.invokeAll(tasks).forEach(ForkJoinTask::join);
            }
        } catch (HttpStatusException httpStatusException) {
            errorLogger(httpStatusException, httpStatusException.getUrl());
            savePageToDBAfterException(url, RequestStatusCode.NOT_FOUND.getCode());
        } catch (NullPointerException nullPointerException) {
            errorLogger(nullPointerException, this.url);
            savePageToDBAfterException(this.url, RequestStatusCode.REQUEST_DENIED.getCode());
        } catch (SocketTimeoutException socketTimeoutException) {
            errorLogger(socketTimeoutException, this.url);
            savePageToDBAfterException(url, RequestStatusCode.REQUEST_TIMEOUT.getCode());
        } catch (IOException ioException) {
            errorLogger(ioException, this.url);
        } catch (SiteNotFoundException siteNotFoundException) {
            log.error("Страница {} не принадлежит индексируемому сайту", siteNotFoundException.getSiteUrl());
        } catch (IllegalArgumentException illegalArgumentException) {
            errorLogger(illegalArgumentException, this.url);
        } catch (Exception exception) {
            log.error("EXCEPTION");
            errorLogger(exception, this.url);
            return JsoupSiteIndexingResponse.failure(RecursiveTaskError.UNEXPECTED_ERROR.getDescription());
        }

        return JsoupSiteIndexingResponse.success(SiteStatus.INDEXED);
    }

    private void addUrl(String url) {
        URI uri = URI.create(url);
        String siteUrl = uri.getScheme().concat("://").concat(uri.getHost()).concat(uri.getPath());
        parsedURLs.add(siteUrl);
    }

    private static <T extends Throwable> void errorLogger(T exception, String exceptionUrl) {
        log.error("{}, url = [{}].\nMessage: {}", exception.getClass().getSimpleName(), exceptionUrl, exception.getMessage());
    }

    private void savePageToDBAfterException(String url, int statusCode) {

        URI uri = URI.create(url);
        String pageScheme = uri.getScheme();
        String pageHost = uri.getHost();
        String pagePath = uri.getPath();

        String siteUrl = pageScheme.concat("://").concat(pageHost);

        pageService.findByPath(pagePath).ifPresentOrElse((foundDto) -> {
                }, () -> {
                    try {
                        SiteDto siteDto = siteService.findByUrlContaining(siteUrl)
                                .orElseThrow(() -> new SiteNotFoundException(siteUrl));

                        PageDto pageDto = PageDto.builder()
                                .code(statusCode)
                                .path(pagePath)
                                .content("")
                                .site(siteDto)
                                .build();

                        pageService.save(pageDto);
                    } catch (SiteNotFoundException siteNotFoundException) {
                        errorLogger(siteNotFoundException, url);
                    }
                }
        );
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