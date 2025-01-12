package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteStatus;
import searchengine.services.IndexingService;
import searchengine.services.jsoup.JSOUPParser;
import searchengine.services.recursive.ForkJoin;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Log4j2
@Transactional
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final JSOUPParser parser;
    private final SiteServiceImpl siteService;

    @Override
    public void startIndexing() {
        List<Site> sitesList = sites.getSites();

        startRecursiveIndexing(sitesList);
    }

    public void startRecursiveIndexing(Collection<Site> sites) {
        sites.forEach(
                (site) -> {
                    String siteURL = site.getUrl();
                    String siteName = site.getName();

                    deleteSiteFromDBByUrl(siteURL);

                    Connection.Response response = parser.executeRequest(siteURL);
                    Collection<String> links = parser.parseAbsoluteLinks(siteURL);

                    SiteDTO beforeSaving = new SiteDTO();

                    beforeSaving.setUrl(response.url().toString());
                    beforeSaving.setSiteStatus(SiteStatus.INDEXING);
                    beforeSaving.setName(siteName);

                    SiteDTO afterSaving = siteService.save(beforeSaving);
                    startForkJoinPool(response, links);
                }
        );
    }

    private void startForkJoinPool(Connection.Response response,
                                   Collection<String> absoluteURLs) {
        ForkJoinPool commonPool = ForkJoinPool.commonPool();
        commonPool.invoke(new ForkJoin(response, absoluteURLs));
    }

    private void deleteSiteFromDBByUrl(String siteUrl) {
        siteService.findByUrl(siteUrl)
                .ifPresentOrElse(
                        siteService::deleteSite,
                        () -> log.info("No data to delete were found."));
    }
}