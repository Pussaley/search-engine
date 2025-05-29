package searchengine.service.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.model.dto.entity.PageDto;
import searchengine.service.impl.PageServiceImpl;
import searchengine.util.jsoup.JSOUPParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Slf4j
public class ForkJoinRecursiveTaskDemo extends RecursiveTask<Boolean> {
    private final JSOUPParser jsoupParser =
            SearchEngineApplicationContext.getBean(JSOUPParser.class);
    private final PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private Connection.Response response;
    private Collection<String> URLs;
    private static Set<String> parsedURLs = new CopyOnWriteArraySet<>();

    public ForkJoinRecursiveTaskDemo(String siteUrl) {
        init(siteUrl);
    }

    public ForkJoinRecursiveTaskDemo(Site site) {
        init(site);
    }

    private void init(Site site) {
        init(site.getUrl());
    }

    private void init(String siteUrl) {
        this.response = jsoupParser.execute(siteUrl);
        this.URLs = jsoupParser.parseAbsoluteLinks(Objects.requireNonNull(response));
    }

    @Override
    protected Boolean compute() {
        List<ForkJoinTask<Boolean>> tasks = new ArrayList<>();
        URLs.removeIf(parsedURLs::contains);

        URLs.forEach(siteURL -> {
            parsedURLs.add(siteURL);

            response = jsoupParser.execute(siteURL);
            PageDto createdPageDto = pageService.createDtoFromJsoupResponse(response);
            pageService.save(createdPageDto);

            tasks.add(new ForkJoinRecursiveTaskDemo(siteURL));
        });

        Collection<ForkJoinTask<Boolean>> forkJoinTasks
                = ForkJoinTask.invokeAll(tasks);

        List<Boolean> result
                = forkJoinTasks.stream()
                .map(ForkJoinTask::join)
                .toList();

        return result.parallelStream().allMatch((r) -> r);
    }
}