package searchengine.service.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.SearchEngineApplicationContext;
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
public class ForkJoinRecursiveTask extends RecursiveTask<Boolean> {
    private final PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private final JSOUPParser jsoupParser =
            SearchEngineApplicationContext.getBean(JSOUPParser.class);
    private Connection.Response response;
    private Collection<String> URLs;
    private ForkJoinTaskStatus taskStatus = ForkJoinTaskStatus.NOT_ACTIVE;
    private static Set<String> parsedURLs = new CopyOnWriteArraySet<>();

    public ForkJoinRecursiveTask(Connection.Response response, Collection<String> links) {
        this.response = Objects.requireNonNull(response);
        this.URLs = links;
    }

    @Override
    protected Boolean compute() {
        List<ForkJoinTask<Boolean>> tasks = new ArrayList<>();
        URLs.removeIf(parsedURLs::contains);

        URLs.forEach(siteURL -> {
            parsedURLs.add(siteURL);

            response = jsoupParser.execute(siteURL);
            Collection<String> links = jsoupParser.parseAbsoluteLinks(response);

            if (!links.isEmpty()) {
                PageDto createdPageDto = pageService.createSiteEntityFromJsoupResponse(response);
                pageService.save(createdPageDto);

                tasks.add(new ForkJoinRecursiveTask(response, links));
            }
        });

        Collection<ForkJoinTask<Boolean>> forkJoinTasks = ForkJoinTask
                .invokeAll(tasks);

        List<Boolean> result = forkJoinTasks.stream()
                .map(ForkJoinTask::join)
                .toList();

        return result.parallelStream().allMatch((r) -> r);
    }

    public void stop() {
        changeState(ForkJoinTaskStatus.NOT_ACTIVE);
    }

    public ForkJoinTaskStatus getState() {
        return taskStatus;
    }

    public void changeState(ForkJoinTaskStatus status) {
        this.taskStatus = status;
    }

}