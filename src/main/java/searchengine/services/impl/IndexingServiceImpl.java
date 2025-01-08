package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.URLStorage;
import searchengine.config.URLUtils;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.recursive.RecursiveActionHandler;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
@Log4j2
@Transactional
@Service
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final SiteServiceImpl siteService;
    private final URLStorage storage;
    private final URLUtils utils;
    private final IndexingServiceImplTEST test;

    @Override
    public void startIndexing() {
        //startRecursiveIndexing();
        test.startIndexing();
    }

    private void startRecursiveIndexing() {
        deleteAllOldData();
        sites.getSites().forEach(site -> {
            RecursiveActionHandler handler = new RecursiveActionHandler(site);
            handler.clear();
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
                .map(URLUtils::repairLink)
                .forEach(siteUrl ->
                        siteService.findByUrl(siteUrl).ifPresentOrElse(
                                siteService::deleteSite,
                                () -> log.info("No data to delete were found.")
                        ));
        log.info("Удалили");
        List<SiteEntity> list = siteService.findAll();
        log.info("В таблице после удаления найдено записей: {}", list.size());
    }
}