package searchengine.service.crud.impl;

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
import searchengine.service.crud.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(timeout = 50)
public class IndexServiceCRUDImpl implements CRUDService<IndexDto> {

    private final IndexRepository indexRepository;
    private final CustomIndexMapper indexMapper;

    @Transactional(readOnly = true)
    @Override
    public Optional<IndexDto> findById(Long id) {
        return Optional.empty();
    }

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
        IndexEntity entity = indexMapper.toEntity(indexDto);
        IndexEntity savedEntity = indexRepository.save(entity);
        return indexMapper.toDto(savedEntity);
    }
}