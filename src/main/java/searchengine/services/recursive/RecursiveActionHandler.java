package searchengine.services.recursive;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import searchengine.config.SearchEngineApplicationContext;
import searchengine.config.Site;
import searchengine.config.URLUtils;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteStatus;
import searchengine.services.impl.PageServiceImpl;
import searchengine.services.impl.SiteServiceImpl;
import searchengine.services.jsoup.JSOUPParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class RecursiveActionHandler extends RecursiveAction {
    /* TODO: переписать с использованием класса URLStorage */

    @Getter
    private static final Collection<String> parsedURLs = new CopyOnWriteArraySet<>();
    private final String urlToParse;
    private PageServiceImpl pageService =
            SearchEngineApplicationContext.getBean(PageServiceImpl.class);
    private SiteServiceImpl siteService =
            SearchEngineApplicationContext.getBean(SiteServiceImpl.class);

    public RecursiveActionHandler(Site site) {
        this(site.getUrl());
    }

    public RecursiveActionHandler(String url) {
        this.urlToParse = URLUtils.repairLink(url);
    }

    @Override
    protected void compute() {
        JSOUPParser parser =
                SearchEngineApplicationContext.getBean(JSOUPParser.class);

        List<RecursiveActionHandler> tasks = new ArrayList<>();

        JSOUPParser.Data data = parser.getData(urlToParse);
        Collection<String> foundURLs = data.getLinks();

        if (!foundURLs.isEmpty() && !parsedURLs.contains(urlToParse)) {

            Connection.Response response = data.getResponse();

            savePage(urlToParse, response);
            foundURLs.stream()
                    .map(RecursiveActionHandler::new)
                    .forEach(tasks::add);

            ForkJoinTask
                    .invokeAll(tasks)
                    .forEach(ForkJoinTask::join);
        }
    }

    public void clear() {
        parsedURLs.clear();
    }

    public SiteDTO findOrCreateSiteByUrl(String siteUrl) {
        return siteService.findByUrl(siteUrl)
                .orElseGet(() -> {
                    SiteDTO siteDTO = new SiteDTO();

                    siteDTO.setUrl(siteUrl);
                    siteDTO.setSiteStatus(SiteStatus.INDEXING);
                    siteDTO.setStatusTime(LocalDateTime.now());
                    siteDTO.setName(URLUtils.parseRootURL(siteUrl));

                    return siteService.save(siteDTO);
                });
    }

    @SneakyThrows
    private void savePage(String page, Connection.Response response) {

        String url = response.url().getProtocol().concat("://").concat(response.url().getHost());
        String content = response.body();
        url = url.endsWith("/") ? url : url.concat("/");

        String relativePage = URLUtils.parseRelURL(page);

        SiteDTO site = findOrCreateSiteByUrl(url);

        PageDTO pageDTO = new PageDTO();
        pageDTO.setPath(relativePage);
        pageDTO.setCode(response.statusCode());
        pageDTO.setContent(content);
        pageDTO.setSite(site);

        pageService.save(pageDTO);
        parsedURLs.add(page);
    }
}