package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.ServiceConnector;
import searchengine.services.recursive.RecursiveActionHandler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@RequiredArgsConstructor
@Log4j2
@Transactional
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final ServiceConnector serviceConnector;

    @Override
    public void startIndexing() {
        deleteAllOldData();
        startRecursiveIndexing();
    }

    private void startRecursiveIndexing() {
        sites.getSites().forEach(site -> {
            RecursiveAction handler = new RecursiveActionHandler(site);
            try {
                ForkJoinPool commonPool = ForkJoinPool.commonPool();
                commonPool.invoke(handler);
            } catch (Exception exception) {
                log.error("Exception: {}", exception.getMessage());
                exception.printStackTrace();
            }

        });
    }

    /**
     * This method deleted all the entities from {@link SiteRepository} and {@link PageRepository}.
     */
    private void deleteAllOldData() {

        sites.getSites().stream()
                .map(Site::getUrl)
                .forEach(siteUrl -> {
                    serviceConnector.findSiteByUrl(siteUrl).ifPresentOrElse(
                            serviceConnector::deleteSite,
                            () -> log.info("No data to delete were found.")
                    );
                });
    }
}