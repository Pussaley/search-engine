package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.exception.SiteNotFoundException;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.recursive.demo.JsoupSiteIndexingResponse;
import searchengine.util.jsoup.JSOUPParser;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class ForkJoinRecursiveTask extends RecursiveTask<JsoupSiteIndexingResponse> {

    private final JSOUPParser jsoupParser =
            SearchEngineApplicationContext.getBean(JSOUPParser.class);
    private final PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private final SiteServiceImpl siteService =
            SearchEngineApplicationContext.getBean(SiteServiceImpl.class);
    private final String url;
    private static final Set<String> parsedURLs = new CopyOnWriteArraySet<>();
    private static final Set<Site> rootSites = new CopyOnWriteArraySet<>();
    @Getter
    @Setter
    private volatile boolean cancel = false;

    public ForkJoinRecursiveTask(String url) {
        this.url = url;
    }

    public ForkJoinRecursiveTask(Site site) {
        this(site.getUrl());
        rootSites.add(site);
    }

    @Override
    protected JsoupSiteIndexingResponse compute() {

        if (cancel) {
            this.cancel(true);
            return JsoupSiteIndexingResponse.failure(TaskStatus.STOPPED_BY_USER.getDescription());
        }

        for (Site site : rootSites) {
            if (url.equalsIgnoreCase(site.getUrl())) {
                try {
                    jsoupParser.parse(url);
                } catch (HttpStatusException e) {
                    log.info("Главная страница сайта {} не отвечает.", url);
                    return JsoupSiteIndexingResponse.failure(TaskStatus.MAIN_PAGE_REJECTED.getDescription());
                }
            }
        }

        List<ForkJoinTask<JsoupSiteIndexingResponse>> tasks = new ArrayList<>();

        Collection<String> pages = jsoupParser.parsePagesAsAbsURLs(url);
        pages.removeIf(parsedURLs::contains);

        if (!pages.isEmpty()) {
            for (String pageUrl : pages) {
                tasks.add(new ForkJoinRecursiveTask(pageUrl));
                parsedURLs.add(pageUrl);
                URI uri;
                PageDto pageDto;
                try {
                    uri = new URI(pageUrl);

                    String content;
                    int statusCode;
                    String pagePath;
                    String siteHost;
                    SiteDto siteDto;
                    try {
                        Optional<Connection.Response> optionalResponse = jsoupParser.parse(pageUrl);
                        if (optionalResponse.isPresent()) {
                            Connection.Response response = optionalResponse.get();
                            statusCode = response.statusCode();
                            content = response.body();
                        } else {
                            continue;
                        }
                    } catch (HttpStatusException httpStatusException) {
                        log.error("Нет доступа к странице <{}>, статус = '{}'", url, httpStatusException.getStatusCode());
                        statusCode = httpStatusException.getStatusCode();
                        content = "";
                    }
                    siteHost = uri.getHost();
                    pagePath = uri.getPath();

                    siteDto = siteService.findByUrlContaining(siteHost)
                            .orElseThrow(() -> {
                                String error = MessageFormat.format("Сайт {0} не найден в БД", siteHost);
                                return new SiteNotFoundException(error);
                            });
                    pageDto = PageDto.builder()
                            .code(statusCode)
                            .content(content)
                            .path(pagePath)
                            .site(siteDto)
                            .build();

                    pageService.findByPath(pageDto.getPath()).ifPresentOrElse(
                            (page) -> log.info("Путь {} уже существует в БД [parsedURLs содержит {}: {}]",
                                    pageDto.getPath(),
                                    pageUrl,
                                    parsedURLs.contains(pageUrl)),
                            () -> {
                                log.info("Сохраняем {}", pageDto.getPath());
                                pageService.save(pageDto);
                            });
                } catch ( NullPointerException nullPointerException) {
                    nullPointerException.printStackTrace();
                    log.error("NULL");
                } catch (Exception exception) {
                    log.error("{}", exception.getClass());
                }
            }
        }
        ForkJoinTask.invokeAll(tasks).
                forEach(ForkJoinTask::join);

        return JsoupSiteIndexingResponse.success(SiteStatus.INDEXED);
    }

    @Getter
    private enum TaskStatus {
        STOPPED_BY_USER("Индексация остановлена пользователем"),
        MAIN_PAGE_REJECTED("Главная страница сайта не отвечает");
        private final String description;

        TaskStatus(String description) {
            this.description = description;
        }
    }
}