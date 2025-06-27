package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.exception.SiteNotFoundException;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.IndexDto;
import searchengine.model.dto.entity.LemmaDto;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.service.impl.IndexServiceImpl;
import searchengine.service.impl.LemmaServiceImpl;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.morphology.LemmaFinder;
import searchengine.util.jsoup.JSOUPParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private final LemmaServiceImpl lemmaService = SearchEngineApplicationContext.getBean(LemmaServiceImpl.class);
    private final IndexServiceImpl indexService = SearchEngineApplicationContext.getBean(IndexServiceImpl.class);
    private final String url;
    private static final Set<String> parsedURLs = new CopyOnWriteArraySet<>();
    @Getter
    @Setter
    private volatile boolean cancel = false;

    public ForkJoinRecursiveTask(String url) {
        this.url = url.endsWith("/") ? url : url.concat("/");
        parsedURLs.add(url);
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

            Connection.Response response = jsoupParser.parseResponse(this.url);
            String rootUrl = Objects.isNull(response) ? URI.create(this.url).getPath() : response.url().getPath();
            if (!parsedURLs.contains(this.url) && rootUrl.equalsIgnoreCase("/") && Objects.isNull(response)) {
                log.error("Главная страница сайта {} недоступна, завершаем индексацию", this.url);
                return JsoupSiteIndexingResponse.failure(RecursiveTaskError.MAIN_PAGE_REJECTED.getDescription());
            }

            Objects.requireNonNull(response);
            Collection<String> nodes = jsoupParser.parseAbsLinks(this.url);

            PageDto pageDto = pageService.createDtoFromJsoupResponse(response);
            String pageRawPath = pageDto.getPath();
            String siteUrl = pageDto.getSite().getUrl();

            PageDto savedPageDto;
            synchronized (this) {
                savedPageDto = pageService.save(pageDto);
            }
            String pageContent = savedPageDto.getContent();
            if (Objects.nonNull(pageContent) && Strings.isNotBlank(pageContent)) {

                LemmaFinder lemmaFinder = LemmaFinder.getInstance();
                Map<String, Integer> lemmas = lemmaFinder.collectLemmas(pageContent);

                for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                    String lemma = entry.getKey();
                    Integer lemmaFrequency = entry.getValue();

                    //TODO: исправить дублирование лемм, возникает исключение IncorrectResultSizeDataAccessException при поиске

                    Optional<LemmaDto> foundLemmaOptional = lemmaService.findByLemma(lemma);
                    LemmaDto lemmaDto;
                    if (foundLemmaOptional.isPresent()) {
                        LemmaDto foundLemma = foundLemmaOptional.get();
                        foundLemma.setFrequency(foundLemma.getFrequency() + 1);
                        lemmaDto = foundLemma;
                    } else {
                        lemmaDto = LemmaDto.builder()
                                .lemma(lemma)
                                .frequency(1)
                                .site(savedPageDto.getSite())
                                .build();
                    }

                    LemmaDto savedLemmaDto = lemmaService.save(lemmaDto);

                    IndexDto indexDto = IndexDto.builder()
                            .pageId(savedPageDto.getId())
                            .lemmaId(savedLemmaDto.getId())
                            .rank((float) lemmaFrequency)
                            .build();

                    indexService.findByLemmaIdAndPageId(indexDto.getLemmaId(), indexDto.getPageId())
                            .orElseGet(() -> indexService.save(indexDto));
                }
            }
            if (!nodes.isEmpty()) {
                nodes.stream().map(link -> {
                            String result = link;

                            URI uri = URI.create(link);
                            String query = uri.getQuery();
                            if (Objects.nonNull(query) && query.split("=")[0].toLowerCase().contains("return")) {
                                String rawPath = uri.getPath().concat(Objects.isNull(uri.getQuery()) ? link.split(uri.getPath())[1] : "");
                                result = uri.getScheme().concat("://").concat(uri.getHost()).concat(rawPath);
                            }
                            return result;
                        }).filter(parsedURLs::add)
                        .map(ForkJoinRecursiveTask::new).forEachOrdered(task -> tasks.add(task.fork()));

                tasks.forEach(ForkJoinTask::join);
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
        } catch (IOException | IllegalArgumentException ioException) {
            errorLogger(ioException, this.url);
        } catch (SiteNotFoundException siteNotFoundException) {
            log.error("Страница {} не принадлежит индексируемому сайту", siteNotFoundException.getSiteUrl());
        } catch (Exception exception) {
            log.error("EXCEPTION");
            errorLogger(exception, this.url);
            return JsoupSiteIndexingResponse.failure(RecursiveTaskError.UNEXPECTED_ERROR.getDescription());
        }

        return JsoupSiteIndexingResponse.success(SiteStatus.INDEXED);
    }

    public synchronized Set<String> getParsedURLs() {
        return parsedURLs;
    }

    public synchronized void prepareForWork() {
        parsedURLs.clear();
    }

    public synchronized int getSize() {
        return parsedURLs.size();
    }

    public static <T extends Exception> void errorLogger(T exception, String exceptionUrl) {
        log.error("{}, url = [{}].\nMessage: {}", exception.getClass().getSimpleName(), exceptionUrl, exception.getMessage());
    }

    private void savePageToDBAfterException(String url, int statusCode) {

        URI uri = URI.create(url);
        String pageScheme = uri.getScheme();
        String pageHost = uri.getHost();
        String pagePath = uri.getPath();
        String pageRawPath = uri.getPath().concat(Objects.isNull(uri.getQuery()) ? "" : url.split(uri.getPath())[1]);

        String siteUrl = pageScheme.concat("://").concat(pageHost);

        pageService.findByPath(pageRawPath).ifPresentOrElse((foundDto) -> {
        }, () -> {
            try {
                log.info("Saving in savePageToDBAfterException");
                SiteDto siteDto = siteService.findByUrlContaining(siteUrl).orElseThrow(() -> new SiteNotFoundException(siteUrl));

                PageDto pageDto = PageDto.builder().code(statusCode).path(pageRawPath).content("").site(siteDto).build();

                pageService.save(pageDto);
            } catch (SiteNotFoundException siteNotFoundException) {
                errorLogger(siteNotFoundException, url);
            }
        });
    }

    @Getter
    public enum RecursiveTaskError {
        MAIN_PAGE_REJECTED("Главная страница сайта не отвечает"), STOPPED_BY_USER("Индексация остановлена пользователем"), UNEXPECTED_ERROR("Неизвестная ошибка");

        private final String description;

        RecursiveTaskError(String description) {
            this.description = description;
        }
    }

    @Getter
    public enum RequestStatusCode {
        NOT_FOUND(404), REQUEST_TIMEOUT(408), REQUEST_DENIED(500);

        private final int code;

        RequestStatusCode(int code) {
            this.code = code;
        }
    }
}