package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.Site;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.URLUtils;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.services.ServiceConnector;
import searchengine.services.jsoup.JSOUPParser;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {
    private static final Collection<String> parsedURLs = new CopyOnWriteArraySet<>();
    private final String urlToParse;

    public RecursiveActionHandler(Site site) {
        this(site.getUrl());
    }

    public RecursiveActionHandler(String url) {
        this.urlToParse = URLUtils.repairLink(url);
    }

    @Override
    protected void compute() {
        ServiceConnector serviceConnector =
                SearchEngineApplicationContext.getBean(ServiceConnector.class);
        JSOUPParser parser =
                SearchEngineApplicationContext.getBean(JSOUPParser.class);

        Collection<String> foundURLs = parser.parseAbsoluteLinks(urlToParse);
        foundURLs.removeIf(parsedURLs::contains);

        if (!foundURLs.isEmpty()) {
            parsedURLs.add(urlToParse);

            Connection.Response response = parser.executeRequest(urlToParse);

            PageDTO pageDTO = new PageDTO();
            pageDTO.setCode(response.statusCode());
            pageDTO.setContent(response.body());
            pageDTO.setPath(parser.parsePath(urlToParse));
            pageDTO.setSite(new SiteDTO()); // ?

            serviceConnector.savePage(new PageDTO());

            log.info("Добавили {}", urlToParse);
            ForkJoinTask
                    .invokeAll(foundURLs.stream()
                            .map(RecursiveActionHandler::new)
                            .toList())
                    .forEach(ForkJoinTask::join);
        }
    }
}