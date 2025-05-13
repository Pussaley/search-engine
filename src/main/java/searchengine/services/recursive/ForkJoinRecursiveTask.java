package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.dao.PageDao;
import searchengine.dao.SiteDao;
import searchengine.dto.entity.PageDTO;
import searchengine.services.impl.PageServiceImpl;
import searchengine.services.impl.SiteServiceImpl;
import searchengine.utils.jsoup.JSOUPParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class ForkJoinRecursiveTask extends RecursiveAction {
    private final PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private final SiteServiceImpl siteService =
            SearchEngineApplicationContext.getBean(SiteServiceImpl.class);
    private final JSOUPParser parser =
            SearchEngineApplicationContext.getBean(JSOUPParser.class);
    private final Connection.Response response;
    private final Collection<String> URLs;
    private static final Set<String> parsedURLs = new CopyOnWriteArraySet<>();

    public ForkJoinRecursiveTask(Connection.Response response, Collection<String> links) {
        this.response = response;
        this.URLs = links;
    }

    @Override
    protected void compute() {
        List<RecursiveAction> tasks = new ArrayList<>();
        URLs.removeIf(parsedURLs::contains);

        URLs.forEach(siteURL -> {
            parsedURLs.add(siteURL);

            Connection.Response response = parser.execute(siteURL);
            Collection<String> links = parser.parseAbsoluteLinksTEST(response);

            pageService.createPageDtoFromResponse(response);

            tasks.add(new ForkJoinRecursiveTask(response, links));
        });

        ForkJoinTask
                .invokeAll(tasks)
                .forEach(ForkJoinTask::join);
    }
}