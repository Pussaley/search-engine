package searchengine.service.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.impl.SiteServiceCRUDImpl;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SitePageServiceTest {
    @PersistenceContext
    private final EntityManager entityManager;
    private final SiteServiceCRUDImpl siteService;

    public void clearDatabaseFromSitePageLemmaIndexEntities(String siteName) {
        siteService.findByName(siteName).ifPresent(dto -> {
            Long siteId = dto.getId();
            entityManager.createQuery("DELETE FROM IndexEntity AS i WHERE i.page.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            entityManager.createQuery("DELETE FROM PageEntity AS p WHERE p.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            entityManager.createQuery("DELETE FROM LemmaEntity AS l WHERE l.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            siteService.deleteById(siteId);
        });
    }

    public void clearDatabaseFromSitePageLemmaIndexEntities(SiteDto siteDto) {
        clearDatabaseFromSitePageLemmaIndexEntities(siteDto.getName());
    }

    public void clearDatabaseFromSitePageLemmaIndexEntities(Site site) {
        clearDatabaseFromSitePageLemmaIndexEntities(site.getName());
    }
}