package searchengine.service.impl.demo;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import searchengine.model.dto.entity.IndexDto;
import searchengine.model.dto.entity.LemmaDto;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.service.impl.IndexServiceImpl;
import searchengine.service.impl.LemmaServiceImpl;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.morphology.LemmaFinder;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.util.jsoup.JSOUPParser;

import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import static searchengine.service.recursive.ForkJoinRecursiveTask.errorLogger;

@Slf4j
public class TaskDemo extends RecursiveTask<Boolean> {

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
    private static final Set<String> parsedPages = Collections.synchronizedSet(new HashSet<>());
    private final Object lock = new Object();

    public TaskDemo(SiteDto siteDto, String url, JSOUPParser jsoupParser, SiteServiceImpl siteService, PageServiceImpl pageService, LemmaServiceImpl lemmaService, IndexServiceImpl indexService) {
        this(siteDto, url, jsoupParser, siteService, pageService, lemmaService, indexService, true);
    }

    public TaskDemo(SiteDto siteDto,
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
            super.cancel(true);
            return false;
        }

        Document document = null;
        LemmaFinder lemmaFinder = null;
        try {
            document = jsoupParser.parseDocument(this.url);

            String formats = "yml|yaml|nc|eps|ws|sql|png|jpg|jpeg|gif|webp|bmp|svg|ico|mp4|webm|ogg|ogv|oga|mp3|wav|pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|zip|rar|7z|tgz|js|css|xml|json|woff|woff2|ttf|otf|apk|exe|bin";
            String cssSelector = "a:not([href~=(#|tel|mailto)|(?i)\\.(".concat(formats).concat(")])");

            Collection<String> pages = new HashSet<>();
            document.select(cssSelector).stream()
                    .map(e -> e.attr("abs:href"))
                    .filter(e -> e.startsWith(siteDto.getUrl()))
                    .map(e -> e.endsWith("/") ? e : e.concat("/"))
                    .filter(e -> !parsedPages.contains(e))
                    .forEach(pages::add);

            Collection<ForkJoinTask<Boolean>> tasks = new HashSet<>();

            for (String page : pages) {
                parsedPages.add(page);
                tasks.add(new TaskDemo(
                        siteDto,
                        page,
                        jsoupParser,
                        siteService,
                        pageService,
                        lemmaService,
                        indexService,
                        false).fork());
                String rawPath = page.replaceFirst(siteDto.getUrl(), "");
                Optional<PageDto> pageOptional = pageService.findByPathAndSiteUrl(rawPath, siteDto.getUrl());
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

                        synchronized (lemmaService) {
                            LemmaDto lemmaDto = lemmaService.findByLemma(lemma)
                                    .orElseGet(() -> LemmaDto.builder().lemma(lemma).site(siteDto).frequency(1).build());

                            LemmaDto savedLemma;
                            if (Objects.isNull(lemmaDto.getId())) {
                                savedLemma = lemmaService.save(lemmaDto);
                            } else {
                                lemmaDto.setFrequency(lemmaDto.getFrequency() + 1);
                                savedLemma = lemmaService.update(lemmaDto);
                            }
/*
                            Optional<LemmaDto> lemmaOptional = lemmaService.findByLemma(lemma).orElseGet();
                            if (lemmaOptional.isPresent()) {
                                LemmaDto lemmaDto = lemmaOptional.get();
                                lemmaId = lemmaDto.getId();
                                lemmaDto.setFrequency(lemmaDto.getFrequency() + 1);
                                lemmaService.update(lemmaDto);
                            } else {
                                LemmaDto savedLemma = lemmaService.save(LemmaDto.builder()
                                        .lemma(lemma)
                                        .frequency(1)
                                        .site(siteDto)
                                        .build());
                                lemmaId = savedLemma.getId();
                            }*/
                            Long lemmaId = savedLemma.getId();
                            synchronized (indexService) {
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
            }
            tasks.forEach(ForkJoinTask::join);
        } catch (IncorrectResultSizeDataAccessException incorrectResultSizeDataAccessException) {
            log.error("Исключение: {}", incorrectResultSizeDataAccessException.getClass().getSimpleName());
            log.error("Сообщение исключения: {}", incorrectResultSizeDataAccessException.getMessage());
            log.error("expectedSize: {}, actualSize: {}", incorrectResultSizeDataAccessException.getExpectedSize(), incorrectResultSizeDataAccessException.getActualSize());
            log.error("stacktrace: ", incorrectResultSizeDataAccessException.getStackTrace());
        } catch (SocketTimeoutException socketTimeoutException) {
            errorLogger(socketTimeoutException, this.url);
            errorSaving(ForkJoinRecursiveTask.RequestStatusCode.REQUEST_TIMEOUT);
        } catch (HttpStatusException httpStatusException) {
            errorLogger(httpStatusException, httpStatusException.getUrl());
            errorSaving(ForkJoinRecursiveTask.RequestStatusCode.NOT_FOUND);
        } catch (Exception exception) {
            log.error("Исключение: {}", exception.getClass().getSimpleName());
            log.error("Сообщение исключения: {}", exception.getMessage());
            log.error("Стэктрейс: {}", exception.getStackTrace());
            return false;
        }

        return true;
    }

    private synchronized void errorSaving(ForkJoinRecursiveTask.RequestStatusCode statusCode) {
        String rawPath = this.url.replaceFirst(siteDto.getUrl(), "");
        PageDto errorDto = PageDto.builder()
                .site(siteDto).content("").path(rawPath).code(statusCode.getCode())
                .build();
        pageService.findByPath(errorDto.getPath()).orElseGet(() -> pageService.save(errorDto));
        siteService.updateStatusTimeById(siteDto.getId());
    }
}