package searchengine.service.crud.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.CustomIndexMapper;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.IndexRepository;
import searchengine.service.CompositeCRUDService;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(timeout = 50)
public class IndexServiceCRUDImpl implements CompositeCRUDService<IndexDto> {

    private final IndexRepository indexRepository;
    private final CustomIndexMapper indexMapper;
    private final ConcurrentMap<Long, Object> pageLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Object> lemmaLocks = new ConcurrentHashMap<>();
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public Optional<IndexDto> findById(Long id) {
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IndexDto> findByPageIdAndLemmaId(Long pageId, Long lemmaId) {
        return indexRepository.findByPageIdAndLemmaId(pageId, lemmaId).map(indexMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<IndexDto> findByPageAndLemma(PageDto pageDto, LemmaDto lemmaDto) {
        return indexRepository.findByPageIdAndLemmaId(pageDto.getId(), lemmaDto.getId()).map(indexMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
    }

    @Override
    public synchronized IndexDto save(IndexDto indexDto) {
        Long pageId = indexDto.getPageId();
        Long lemmaId = indexDto.getLemmaId();

        Long firstLockId = Math.min(pageId, lemmaId);
        Long secondLockId = Math.max(pageId, lemmaId);

        Object firstLock = getLock(firstLockId, pageId.equals(firstLockId) ? pageLocks : lemmaLocks);
        Object secondLock = getLock(secondLockId, pageId.equals(secondLockId) ? pageLocks : lemmaLocks);

        synchronized (firstLock) {
            synchronized (secondLock) {
                IndexEntity entity = indexMapper.toEntity(indexDto);
                IndexEntity savedEntity = indexRepository.save(entity);
                return indexMapper.toDto(savedEntity);
            }
        }
    }

    private Object getLock(Long id, ConcurrentMap<Long, Object> lockMap) {
        return lockMap.computeIfAbsent(id, k -> new Object());
    }
}