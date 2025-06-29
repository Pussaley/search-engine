package searchengine.service.recursive;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.impl.IndexServiceImpl;
import searchengine.service.impl.LemmaServiceImpl;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.morphology.LemmaFinder;
import searchengine.util.jsoup.JSOUPParser;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
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
    @Getter
    @Setter
    private static volatile boolean cancelRecursiveTask = false;
    @Getter
    @Setter
    private final boolean newIndexing;
    private static final Set<String> parsedPages = ConcurrentHashMap.newKeySet();

    public RecursiveSiteCrawler(SiteDto siteDto, String url, JSOUPParser jsoupParser, SiteServiceImpl siteService, PageServiceImpl pageService, LemmaServiceImpl lemmaService, IndexServiceImpl indexService) {
        this(siteDto, url, jsoupParser, siteService, pageService, lemmaService, indexService, true);
    }

    public RecursiveSiteCrawler(SiteDto siteDto,
                                String url,
                                JSOUPParser jsoupParser,
                                SiteServiceImpl siteService,
                                PageServiceImpl pageService,
                                LemmaServiceImpl lemmaService,
                                IndexServiceImpl indexService,
                                boolean newIndexing) {
        this.siteDto = siteDto;
        this.url = url;
        this.jsoupParser = jsoupParser;
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.newIndexing = newIndexing;

        if (newIndexing)
            parsedPages.removeIf(elem -> elem.contains(siteDto.getUrl()));
    }

    @Override
    protected Boolean compute() {

        if (cancelRecursiveTask) {
            cancel(true);
            return false;
        }

        Document document = null;
        LemmaFinder lemmaFinder = null;
        try {
            document = jsoupParser.parseDocument(this.url);

            String formats = "yml|yaml|nc|eps|ws|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
            String cssSelector = "a:not([href~=(#|tel|mailto)|(?i)\\.(".concat(formats).concat(")])");

            Collection<String> pages = document.select(cssSelector).stream()
                    .map(e -> e.attr("abs:href"))
                    .filter(e -> e.startsWith(siteDto.getUrl()))
                    .map(e -> e.endsWith("/") ? e : e.concat("/"))
                    .filter(parsedPages::add)
                    .collect(Collectors.toCollection(HashSet::new));

            //Collection<ForkJoinTask<Boolean>> tasks = new HashSet<>();

            ForkJoinTask<Boolean>[] tasksArray = new ForkJoinTask[pages.size()];

            int pIndex = 0;
            for (String page : pages) {
                tasksArray[pIndex++] = new RecursiveSiteCrawler(
                        siteDto,
                        page,
                        jsoupParser,
                        siteService,
                        pageService,
                        lemmaService,
                        indexService,
                        false).fork();
                String rawPath = page.replaceFirst(siteDto.getUrl(), "");
                Optional<PageDto> pageOptional;
                synchronized (rawPath.intern()) {
                    pageOptional = pageService.findByPathAndSiteUrl(rawPath, siteDto.getUrl());
                }
                if (pageOptional.isEmpty()) {
                    String content = document.html();
                    PageDto pageDto = PageDto.builder().site(siteDto).content(content).path(rawPath).code(200).build();
                    PageDto savedPageDto = pageService.save(pageDto);
                    siteService.updateStatusTimeById(siteDto.getId());
                    Long pageId = savedPageDto.getId();
                    lemmaFinder = LemmaFinder.getInstance();
                    Map<String, Integer> lemmas = lemmaFinder.collectLemmas(content);
                    for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                        String lemma = entry.getKey();
                        Integer lemmaCount = entry.getValue();

                        synchronized (lemma.intern()) {
                            LemmaDto lemmaDto = lemmaService.findByLemma(lemma)
                                    .orElseGet(() -> LemmaDto.builder()
                                            .lemma(lemma)
                                            .site(siteDto)
                                            .frequency(1)
                                            .build());

                            LemmaDto savedLemma;
                            if (Objects.isNull(lemmaDto.getId())) {
                                savedLemma = lemmaService.save(lemmaDto);
                            } else {
                                lemmaDto.setFrequency(lemmaDto.getFrequency() + 1);
                                savedLemma = lemmaService.update(lemmaDto);
                            }

                            Long lemmaId = savedLemma.getId();
                            indexService.findByLemmaIdAndPageId(lemmaId, pageId).orElseGet(() ->
                                    indexService.save(IndexDto.builder()
                                            .lemmaId(lemmaId)
                                            .pageId(pageId)
                                            .rank((float) lemmaCount)
                                            .build()));
                        }
                    }
                }
            }
            Arrays.stream(tasksArray).forEach(ForkJoinTask::join);
        } catch (IncorrectResultSizeDataAccessException incorrectResultSizeDataAccessException) {
            log.error("Исключение: {}", incorrectResultSizeDataAccessException.getClass().getSimpleName());
            log.error("Сообщение исключения: {}", incorrectResultSizeDataAccessException.getMessage());
            log.error("expectedSize: {}, actualSize: {}", incorrectResultSizeDataAccessException.getExpectedSize(), incorrectResultSizeDataAccessException.getActualSize());
            log.error("stacktrace: ", incorrectResultSizeDataAccessException.getStackTrace());
        } catch (SocketTimeoutException socketTimeoutException) {
            errorLogger(socketTimeoutException, this.url);
            errorSaving(RequestStatusCode.REQUEST_TIMEOUT);
        } catch (HttpStatusException httpStatusException) {
            errorLogger(httpStatusException, httpStatusException.getUrl());
            errorSaving(RequestStatusCode.NOT_FOUND);
        } catch (Exception exception) {
            log.error("Исключение: {}", exception.getClass().getSimpleName());
            log.error("Сообщение исключения: {}", exception.getMessage());
            log.error("Стэктрейс: {}", exception.getStackTrace());
            return false;
        }

        return true;
    }

    private static <T extends Exception> void errorLogger(T exception, String url) {
        log.error("Возникло исключение {} при обработке страницы: {}", exception.getClass().getSimpleName(), url);
        log.error("PrintStackTrace:");
        exception.printStackTrace();
    }

    private synchronized void errorSaving(RequestStatusCode statusCode) {
        String rawPath = this.url.replaceFirst(siteDto.getUrl(), "");
        PageDto errorDto = PageDto.builder()
                .site(siteDto).content("").path(rawPath).code(statusCode.getCode())
                .build();
        pageService.findByPath(errorDto.getPath()).orElseGet(() -> pageService.save(errorDto));
        siteService.updateStatusTimeById(siteDto.getId());
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