package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.recursive.ActionImpl;
import searchengine.services.recursive.RecursiveActionHandler;


@RequiredArgsConstructor
@Log4j2
@Transactional
@Service
public class IndexingServiceImpl implements IndexingService, CommandLineRunner {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public void startIndexing() {
        deleteAllOldData();
        sites.getSites().forEach(site -> {
            ActionImpl action = new ActionImpl(new RecursiveActionHandler(site));
            action.start();
        });
    }

    /**
     *This method deleted all the entities from {@link SiteRepository} and {@link PageRepository}.
     */
    public void deleteAllOldData() {

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

    private void createNewRecordsInSiteRepository() {
        sites.getSites().forEach(element -> {

                    SiteEntity site = new SiteEntity();
                    site.setUrl(element.getUrl());
                    site.setName(element.getName());
                    site.setStatus(SiteStatus.INDEXING);
                    siteRepository.save(site);

                    siteRepository.findByUrl(site.getUrl()).ifPresent(
                            opt -> {
                                Page page = new Page();
                                page.setSite(opt);
                                page.setPath("/");
                                page.setCode(200);
                                page.setContent("<html>");

                                pageRepository.save(page);
                            }
                    );
                }
        );
    }

    @Override
    public void run(String... args) throws Exception {
        startIndexing();
    }
}