package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.IndexEntityMapper;
import searchengine.model.dto.entity.IndexDto;
import searchengine.model.entity.IndexEntity;
import searchengine.repository.IndexRepository;
import searchengine.service.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class IndexServiceImpl implements CRUDService<IndexDto> {

    private final IndexRepository indexRepository;
    private final IndexEntityMapper indexMapper;

    @Override
    public Optional<IndexDto> findById(Long id) {
        return indexRepository.findById(id).map(indexMapper::toDto);
    }

    @Override
    public boolean deleteById(Long id) {
        indexRepository.deleteById(id);
        return !indexRepository.existsById(id);
    }

    @Override
    public IndexDto save(IndexDto indexDto) {
        IndexEntity entity = indexMapper.toEntity(indexDto);
        IndexEntity savedEntity = indexRepository.save(entity);
        return indexMapper.toDto(savedEntity);
    }

    public void deleteIndexesBySiteId(Long siteId) {
        indexRepository.deleteIndexesByPageId(siteId);
        indexRepository.flush();
    }

    public Optional<IndexDto> findByLemmaIdAndPageId(Long lemmaId, Long pageId) {
        return indexRepository.findByLemmaIdAndPageId(lemmaId, pageId)
                .map(indexMapper::toDto);
    }
}