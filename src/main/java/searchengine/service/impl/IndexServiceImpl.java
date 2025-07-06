package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.IndexEntityMapper;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.IndexDtoKey;
import searchengine.repository.IndexRepository;
import searchengine.service.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class IndexServiceImpl implements CRUDService<IndexDto, IndexDtoKey> {

    private final IndexRepository indexRepository;
    private final IndexEntityMapper indexMapper;

    @Override
    public Optional<IndexDto> findById(IndexDtoKey id) {
        return indexRepository
                .findByLemmaIdAndPageId(id.getLemmaId(), id.getPageId())
                .map(indexMapper::toDto);
    }

    @Override
    public void deleteById(IndexDtoKey id) {
        indexRepository.deleteByLemmaIdAndPageId(id.getLemmaId(), id.getPageId());
    }

    @Override
    public IndexDto save(IndexDto indexDto) {
        IndexEntity entity = indexMapper.toEntity(indexDto);
        IndexEntity savedEntity = indexRepository.save(entity);
        return indexMapper.toDto(savedEntity);
    }
}