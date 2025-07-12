package searchengine.service.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.model.entity.dto.PageDto;
import searchengine.service.impl.PageServiceImpl;
import searchengine.service.impl.SiteServiceImpl;

@Service
@RequiredArgsConstructor
@Transactional
public class SitePageServiceTest {
    @PersistenceContext
    private final EntityManager entityManager;
    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;

    public PageDto savePage(PageDto pageDto) {
        PageDto savedPage = pageService.save(pageDto);
        siteService.updateStatusTimeById(savedPage.getSite().getId());
        return savedPage;
    }

    public void clearDatabaseFromSitePageLemmaIndexEntities(Site site) {
        siteService.findByName(site.getName()).ifPresent( siteDto -> {
            Long siteId = siteDto.getId();
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
}