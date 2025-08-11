package searchengine.service.crud.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.PageRepository;
import searchengine.service.crud.CRUDService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(timeout = 15)
public class PageServiceCRUDImpl implements CRUDService<PageDto> {

    private final PageMapper pageMapper;
    private final PageRepository pageRepository;
    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public PageEntity getReferenceById(Long pageId) {
        return pageRepository.getReferenceById(pageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id).map(pageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PageDto> findByPathAndSiteId(String path, Long siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId).map(pageMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
        entityManager.createNativeQuery("update lemmas as l set l.frequency = l.frequency - 1 where id IN (select lemma_id from indexes as i where i.page_id = ?)")
                .setParameter(1, id)
                .executeUpdate();

        entityManager.createNativeQuery("delete from indexes as i where i.page_id = ?")
                .setParameter(1, id)
                .executeUpdate();

        entityManager.createNativeQuery("delete from lemmas as l where l.frequency = 0")
                .executeUpdate();

        entityManager.createNativeQuery("delete from pages as p where p.id = ?")
                .setParameter(1, id)
                .executeUpdate();
    }

    public void delete(PageDto pageDto) {
        deleteById(pageDto.getId());

        entityManager.createNativeQuery("update sites as s set s.status_time = now() where s.id = ?")
                .setParameter(1, pageDto.getSite().getId())
                .executeUpdate();
    }

    public synchronized PageDto save(PageDto pageDto) {
        PageEntity pageEntity = pageMapper.toEntity(pageDto);

        SiteEntity site = entityManager.getReference(SiteEntity.class, pageDto.getSite().getId());
        site.setStatusTime(LocalDateTime.now());
        pageEntity.setSite(site);

        PageEntity savedPage = pageRepository.save(pageEntity);
        return pageMapper.toDto(savedPage);
    }
}