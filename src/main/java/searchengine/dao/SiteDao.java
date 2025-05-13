package searchengine.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.services.impl.PageServiceImpl;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class SiteDao {
    private final SiteRepository siteRepository;
    private final PageServiceImpl pageService;

    public void clearDatabaseBySiteName(String siteName) {
        siteRepository
                .findByName(siteName)
                .map(SiteEntity::getId)
                .ifPresent(pageService::deletePagesBySiteId);

        siteRepository.deleteByName(siteName);
    }
    public void clearDatabaseBySiteId(Long id) {
        siteRepository
                .findById(id)
                .map(SiteEntity::getId)
                .ifPresent(pageService::deletePagesBySiteId);

        siteRepository.deleteById(id);
    }
}