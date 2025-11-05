package searchengine.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.CustomIndexMapper;
import searchengine.mapper.LemmaEntityMapper;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;

import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Repository
@Transactional(readOnly = true)
public class DataSearchDAO {
    @PersistenceContext
    private final EntityManager entityManager;
    private final PageMapper pageMapper;
    private final LemmaEntityMapper lemmaMapper;
    private final CustomIndexMapper indexMapper;

    public Long getSiteIdBySiteUrl(String siteUrl) {
        return entityManager.createQuery("select s.id from SiteEntity as s where s.url = :siteUrl",
                        Long.class)
                .setParameter("siteUrl", siteUrl)
                .getSingleResult();
    }

    public List<IndexDto> findIndexesByPageIdAndLemmas(Long pageId, List<String> lemmas) {
        return entityManager.createQuery("""
                                select i 
                                from IndexEntity as i
                                where i.page.id = :pageId and i.lemma.lemma 
                                in :lemmas
                                """,
                        IndexEntity.class)
                .setParameter("pageId", pageId)
                .setParameter("lemmas", lemmas)
                .getResultList().stream()
                .filter(Objects::nonNull)
                .map(indexMapper::toDto)
                .toList();
    }

    public Integer countPagesBySiteId(Long siteId) {
        Long count = (Long) entityManager.createNativeQuery("select count(*) from pages as p where p.site_id = ?")
                .setParameter(1, siteId)
                .getSingleResult();
        return count.intValue();
    }

    public List<LemmaDto> findLemmasOrderByFrequency(Long siteId, List<String> lemmas) {
        return entityManager.createQuery("""
                                select l 
                                from LemmaEntity as l 
                                where l.lemma in (:lemmas) and l.site.id = :siteId 
                                order by l.frequency""",
                        LemmaEntity.class)
                .setParameter("lemmas", lemmas)
                .setParameter("siteId", siteId)
                .getResultList().stream()
                .filter(Objects::nonNull)
                .map(lemmaMapper::toDto)
                .toList();
    }

    public List<PageDto> findPagesByLemmaId(Long lemmaId) {

        return entityManager.createQuery("""
                                select p
                                from PageEntity as p 
                                where p.id in (
                                            select i.page.id 
                                            from IndexEntity as i 
                                            where i.lemma.id = :lemmaId)""",
                        PageEntity.class)
                .setParameter("lemmaId", lemmaId)
                .getResultList().stream()
                .filter(Objects::nonNull)
                .map(pageMapper::toDto)
                .toList();
    }

    public List<PageDto> findPagesByLemmaIdInPages(Long lemmaId, List<Long> ids) {

        return entityManager.createQuery("""
                                select p
                                from PageEntity as p 
                                where p.id in (
                                        select i.page.id
                                        from IndexEntity as i
                                        where i.lemma.id = :lemmaId and i.page.id in :ids)""",
                        PageEntity.class)
                .setParameter("lemmaId", lemmaId)
                .setParameter("ids", ids)
                .getResultList().stream()
                .filter(Objects::nonNull)
                .map(pageMapper::toDto)
                .toList();
    }
}