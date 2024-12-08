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
import searchengine.services.recursive.RecursiveAction;
import searchengine.services.recursive.RecursiveActionImpl;
import searchengine.services.recursive.RecursiveActionHandler;

@RequiredArgsConstructor
@Log4j2
@Transactional
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public void startIndexing() {
        deleteAllOldData();
        sites.getSites().forEach(site -> {
            RecursiveAction action = new RecursiveActionImpl(new RecursiveActionHandler(site));
            action.start();
        });
    }

    /**
     * This method deleted all the entities from {@link SiteRepository} and {@link PageRepository}.
     */
    private void deleteAllOldData() {

        sites.getSites().stream()
                .map(Site::getUrl)
                .forEach(siteUrl -> {
                    siteRepository.findByUrl(siteUrl).ifPresentOrElse(
                            entity -> {
                                entity.clearPages();
                                siteRepository.delete(entity);
                                siteRepository.flush();
                            },
                            () -> log.info("No data to delete were found.")
                    );
                });
    }
}