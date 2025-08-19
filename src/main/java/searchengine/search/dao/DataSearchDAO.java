package searchengine.search.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.CustomIndexMapper;
import searchengine.mapper.LemmaEntityMapper;
import searchengine.mapper.PageMapper;
import searchengine.mapper.SiteMapper;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Repository
@Transactional
public class DataSearchDAO {
    @PersistenceContext
    private final EntityManager entityManager;
    private final SiteMapper siteMapper;
    private final PageMapper pageMapper;
    private final LemmaEntityMapper lemmaMapper;
    private final CustomIndexMapper indexMapper;

    public SiteDto findSiteDtoBySiteUrl(String siteUrl) {
        SiteEntity site =
                (SiteEntity) entityManager.createNativeQuery("select * from sites as s where s.url = ?",
                                SiteEntity.class)
                        .setParameter(1, siteUrl)
                        .getSingleResult();

        return siteMapper.toDTO(site);
    }

    public Long getSiteIdBySiteUrl(String siteUrl) {
        return entityManager.createQuery("select s.id from SiteEntity as s where s.url = :siteUrl",
                        Long.class)
                .setParameter("siteUrl", siteUrl)
                .getSingleResult();
    }

    public Optional<LemmaDto> findLemmasByLemmaAndSiteId(String lemma, Long siteId) {
        List<LemmaEntity> lemmas = entityManager
                .createQuery("select l from LemmaEntity as l where l.site.id = :siteId and l.lemma = :lemma",
                        LemmaEntity.class)
                .setParameter("lemma", lemma)
                .setParameter("siteId", siteId)
                .getResultList();

        LemmaEntity lemmaEntity = lemmas.isEmpty() ? null : lemmas.get(0);

        return Optional.ofNullable(lemmaMapper.toDto(lemmaEntity));
    }

    public List<PageDto> findPagesByLemmaAndSiteId(String lemma, Long siteId) {
        return entityManager.createQuery("""
                                select p
                                from PageEntity as p
                                where p.id in (select i.page.id
                                               from IndexEntity as i
                                               where i.lemma.id =
                                                     (select l.id from LemmaEntity as l where l.lemma = :lemma and l.site.id = :siteId))""",
                        PageEntity.class)
                .setParameter("lemma", lemma)
                .setParameter("siteId", siteId)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(pageMapper::toDto)
                .toList();
    }

    public List<PageDto> findPagesByLemmaAndSiteId(String lemma, Long siteId, List<PageDto> indexes) {
        List<Long> pagesIds = indexes.stream().map(PageDto::getId).toList();
        return entityManager.createQuery("""
                                select p
                                from PageEntity as p
                                where p.id in 
                                            (select i.page.id
                                            from IndexEntity as i
                                            where i.lemma.id =
                                                        (select l.id from LemmaEntity as l where l.lemma = :lemma and l.site.id = :siteId)
                                            and i.page.id in :pageIds)""",
                        PageEntity.class)
                .setParameter("lemma", lemma)
                .setParameter("siteId", siteId)
                .setParameter("pageIds", pagesIds)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(pageMapper::toDto)
                .toList();
    }

    public List<IndexDto> findIndexesByLemmaAndSiteId(String lemma, Long siteId) {
        return entityManager.createQuery("""
                                select i
                                from IndexEntity as i
                                where i.lemma.id =
                                            (select l.id from LemmaEntity as l where l.lemma = :lemma and l.site.id = :siteId)""",
                        IndexEntity.class)
                .setParameter("lemma", lemma)
                .setParameter("siteId", siteId)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(indexMapper::toDto)
                .toList();
    }

    public List<IndexDto> findIndexesByLemmaAndSiteId(String lemma, Long siteId, List<IndexDto> indexes) {
        List<Long> pagesIds = indexes.stream().map(IndexDto::getPageId).toList();
        return entityManager.createQuery("""
                                select i
                                from IndexEntity as i
                                where i.lemma.id =
                                            (select l.id from LemmaEntity as l where l.lemma = :lemma and l.site.id = :siteId)
                                and i.page.id in :pageIds""",
                        IndexEntity.class)
                .setParameter("lemma", lemma)
                .setParameter("siteId", siteId)
                .setParameter("pageIds", pagesIds)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(indexMapper::toDto)
                .toList();
    }

    public List<IndexDto> findIndexesByPageIdAndLemmas(Long pageId, List<String> lemmas) {
        return entityManager.createQuery("""
                                select i from IndexEntity as i
                                where i.page.id = :pageId and i.lemma.lemma in :lemmas
                                """,
                        IndexEntity.class)
                .setParameter("pageId", pageId)
                .setParameter("lemmas", lemmas)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(indexMapper::toDto)
                .toList();
    }
}