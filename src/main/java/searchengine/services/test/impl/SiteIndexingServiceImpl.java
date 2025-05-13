package searchengine.services.test.impl;

import lombok.RequiredArgsConstructor;
import searchengine.config.Site;
import searchengine.dto.entity.SiteDTO;
import searchengine.services.test.PageIndexingService;
import searchengine.services.test.SiteIndexingService;

@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService<SiteDTO> {
    private final PageIndexingService pageIndexingService;

    @Override
    public void start(Site site) {
        pageIndexingService.startIndexing();
    }
}