package searchengine.services.recursive;

import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.exceptions.SiteNotCreatedException;
import searchengine.services.impl.PageServiceImpl;
import searchengine.services.impl.SiteServiceImpl;
import searchengine.services.jsoup.JSOUPParser;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class ForkJoin extends RecursiveAction {
    private final Collection<String> URLs;
    private static final Collection<String> parsedURLs = new CopyOnWriteArraySet<>();
    private final PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private final SiteServiceImpl siteService =
            SearchEngineApplicationContext.getBean(SiteServiceImpl.class);

    public ForkJoin(Connection.Response response, Collection<String> absoluteURLs) {
        saveDataToDB(response);
        this.URLs = absoluteURLs;
    }

    @Override
    protected void compute() {
        JSOUPParser parser =
                SearchEngineApplicationContext.getBean(JSOUPParser.class);


        List<RecursiveAction> tasks = new ArrayList<>();
        URLs.removeIf(parsedURLs::contains);

        URLs.forEach(siteURL -> {

            Connection.Response response = parser.executeRequest(siteURL);
            Collection<String> links = parser.parseAbsoluteLinks(siteURL);
            tasks.add(new ForkJoin(response, links));

        });

        ForkJoinTask
                .invokeAll(tasks)
                .forEach(ForkJoinTask::join);
    }

    @SneakyThrows
    @Transactional
    public void saveDataToDB(Connection.Response response) {
        Document document = response.parse();
        URL url = response.url();
        String path = url.getPath();

        SiteDTO siteDTO = siteService
                .findByUrl(url.getHost())
                .orElseThrow(SiteNotCreatedException::new);

        PageDTO pageDTO = new PageDTO();
        pageDTO.setCode(response.statusCode());
        pageDTO.setPath(path);
        pageDTO.setContent(document.toString());
        pageDTO.setSite(siteDTO);

        pageService.save(pageDTO);
        log.info("Сохранили");
    }
}