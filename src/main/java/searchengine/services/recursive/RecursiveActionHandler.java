package searchengine.services.recursive;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.config.URLUtils;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.services.CRUDService;
import searchengine.services.impl.PageServiceImpl;
import searchengine.services.jsoup.JSOUPParser;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {
    public static final Collection<String> parsedURLs = new CopyOnWriteArraySet<>();
    private final String urlToParse;

    public RecursiveActionHandler(Site site) {
        this(site.getUrl());
    }

    public RecursiveActionHandler(String url) {
        this.urlToParse = URLUtils.repairLink(url);
    }

    @Override
    protected void compute() {
        CRUDService<PageDTO> pageService =
                SearchEngineApplicationContext.getBean(PageServiceImpl.class);
        JSOUPParser parser =
                SearchEngineApplicationContext.getBean(JSOUPParser.class);

        Collection<String> foundURLs = parser.parseAbsoluteLinks(urlToParse);

        foundURLs.removeIf(parsedURLs::contains);

        if (!foundURLs.isEmpty()) {
            parsedURLs.add(urlToParse);
/*
            Connection.Response response = parser.executeRequest(urlToParse);

            PageDTO pageDTO = new PageDTO();
            pageDTO.setCode(response.statusCode());
            pageDTO.setContent(response.body());
            pageDTO.setPath(parser.parsePath(urlToParse));
            pageDTO.setSite(new SiteDTO()); // ?

            pageService.save(pageDTO);*/

            log.info("Добавили {}", urlToParse);
            ForkJoinTask
                    .invokeAll(foundURLs.stream()
                            .map(RecursiveActionHandler::new)
                            .toList())
                    .forEach(ForkJoinTask::join);
        }
    }
}