package searchengine.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.PageRepository;
import searchengine.service.CRUDService;

import java.util.List;
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
    public List<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path).stream().map(pageMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PageDto> findByPathAndSiteId(String path, Long siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId).map(pageMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {

        entityManager.createQuery("delete from IndexEntity i where i.page.id = :pageId")
                .setParameter("pageId", id)
                .executeUpdate();

/*        List<IndexEntity> inds = entityManager.createQuery("select i from IndexEntity i where i.page.id = :pageId", IndexEntity.class)
                .setParameter("pageId", id).getResultList();

        int totalRows = 0;

        for (IndexEntity index : inds) {
            Long lemmaId = index.getLemma().getId();
            Integer amount = index.getRank().intValue();
            totalRows += entityManager.createQuery("update LemmaEntity l set l.frequency = l.frequency - :amount where l.id = :id")
                    .setParameter("id", lemmaId)
                    .setParameter("amount", amount)
                    .executeUpdate();

            entityManager.remove(index);
        }

        log.info("Обновлено записей: {}", totalRows);*/
        PageEntity pageEntity = entityManager.getReference(PageEntity.class, id);
        entityManager.remove(pageEntity);
    }

    public PageDto save(PageDto pageDTO) {
        PageEntity pageEntity = pageMapper.toEntity(pageDTO);
        PageEntity savedPage = pageRepository.save(pageEntity);
        return pageMapper.toDto(savedPage);
    }
}