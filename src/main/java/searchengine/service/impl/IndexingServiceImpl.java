package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.dto.response.Response;
import searchengine.model.dto.response.ResponseSuccessDto;
import searchengine.service.IndexingService;
import searchengine.service.impl.indexing.IndexingServiceStarter;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.util.jsoup.JSOUPParser;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Log4j2
@Service
@Transactional
@Profile("prod")
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final JSOUPParser parser;
    private final IndexingServiceStarter searchIndexingService;
    private final SiteServiceImpl siteService;

    @Override
    public Response startIndexing() {
        List<Site> sitesList = sites.getSites();

        startRecursiveIndexing(sitesList);
        return new ResponseSuccessDto(true);
    }

    @Override
    public Response stopIndexing() {
        throw new UnsupportedOperationException("UNDER CONSTRUCTION");
    }

    private void startRecursiveIndexing(Collection<Site> sites) {

        clearSitesTableInDatabase(sites);

        Runnable runnable = () -> {
            sites.parallelStream()
                    .forEach(
                            (site) -> {
                                String siteURL = site.getUrl();
                                String siteName = site.getName();

                                SiteStatus status;
                                SiteDto siteDTO = new SiteDto();

                                Connection.Response response = parser.execute(siteURL);

                                if (Objects.nonNull(response)) {

                                    Collection<String> links = parser.parseAbsoluteLinks(siteURL);

                                    ForkJoinPool pool = ForkJoinPool.commonPool();
                                    pool.execute(new ForkJoinRecursiveTask(response, links));

                                    String host = response.url().getHost();

                                    siteDTO = siteService
                                            .findByUrlContaining(host)
                                            .orElseGet(
                                                    () -> {
                                                        SiteDto savedSite =
                                                                siteService.createSiteEntityFromJsoupResponse(response);

                                                        log.info("IndexingServiceImpl | {}", savedSite.getUrl());

                                                        return siteService.save(savedSite);
                                                    }
                                            );
                                } else {
                                    status = SiteStatus.FAILED;
                                    siteDTO.setLastError("Null");
                                    siteDTO.setSiteStatus(status);
                                }

                                siteService.findByNameContaining(siteName).ifPresentOrElse(
                                        (indexedSite) -> {
                                            indexedSite.setSiteStatus(SiteStatus.INDEXED);
                                            siteService.updateSite(indexedSite);
                                        },
                                        () -> log.info("No site found, the input was: {}", siteName)
                                );
                            }
                    );

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (
                    InterruptedException e) {
                log.info(e.getMessage());
            }


        };

        runnable.run();

        log.info("IndexingServiceImpl, line 106");
    }

    public void clearSitesTableInDatabase(Collection<Site> sites) {
        sites.stream()
                .map(Site::getName)
                .forEach(siteService::clearDatabaseBySiteName);
    }
}