package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dao.SiteDao;
import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteStatus;
import searchengine.services.IndexingService;
import searchengine.services.recursive.ForkJoinRecursiveTask;
import searchengine.utils.jsoup.JSOUPParser;
import searchengine.utils.url.URLUtils;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Log4j2
@Service
@Transactional
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final JSOUPParser parser;
    private final SiteServiceImpl siteService;
    private final SiteDao siteDao;

    @Override
    public void startIndexing() {
        List<Site> sitesList = sites.getSites();

        startRecursiveIndexing(sitesList);
    }

    private void startRecursiveIndexing(Collection<Site> sites) {

        clearSitesTableInDatabase(sites);

        sites.forEach(
                        (site) -> {
                            String siteURL = site.getUrl();
                            String siteName = site.getName();

                            SiteStatus status = null;

                            Connection.Response response = parser.execute(siteURL);

                            if (Objects.nonNull(response)) {
                                Collection<String> links = parser.parseAbsoluteLinksTEST(siteURL);

                                ForkJoinPool pool = ForkJoinPool.commonPool();
                                pool.execute(new ForkJoinRecursiveTask(response, links));

                                String host = response.url().getHost();

                                SiteDTO siteDTO = siteService
                                        .findByUrlContaining(host)
                                        .orElseGet(
                                                () -> {

                                                    SiteDTO mainSite = new SiteDTO();

                                                    mainSite.setId(999L);

                                                    URL responseUrl = response.url();
                                                    String responseUrlString = response.url().toString();
                                                    String hostName = responseUrl.getHost();

                                                    int hostLength = hostName.split("\\.").length;

                                                    String url = URLUtils.removeEndBackslash(responseUrlString);
                                                    String name = hostLength > 1
                                                            ? hostName.split("\\.")[hostLength-2]
                                                            : hostName;

                                                    mainSite.setUrl(url);
                                                    mainSite.setName(name);
                                                    mainSite.setSiteStatus(SiteStatus.INDEXING);

                                                    return siteService.save(mainSite);
                                                }
                                        );
                            } else {
                                status = SiteStatus.FAILED;
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
        } catch (InterruptedException e) {
            log.info(e.getMessage());
        }

        log.info("The program is over.");
    }

    public void clearSitesTableInDatabase(Collection<Site> sites) {
        sites.stream()
                .map(Site::getName)
                .forEach(siteDao::clearDatabaseBySiteName);
    }
}