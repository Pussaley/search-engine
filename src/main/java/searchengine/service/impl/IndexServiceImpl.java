package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.mapper.CustomIndexMapper;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.IndexRepository;
import searchengine.service.CompositeCRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements CompositeCRUDService<IndexDto> {

    private final IndexRepository indexRepository;
    private final CustomIndexMapper indexMapper;

    @Override
    public Optional<IndexDto> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public Optional<IndexDto> findByPageIdAndLemmaId(Long pageId, Long lemmaId) {
        return indexRepository.findByPageIdAndLemmaId(pageId, lemmaId).map(indexMapper::toDto);
    }

    public Optional<IndexDto> findByPageAndLemma(PageDto pageDto, LemmaDto lemmaDto) {
        return indexRepository.findByPageIdAndLemmaId(pageDto.getId(), lemmaDto.getId()).map(indexMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
    }

    @Override
    public IndexDto save(IndexDto indexDto) {

        IndexEntity entity = indexMapper.toEntity(indexDto);

        IndexEntity savedEntity = indexRepository.save(entity);
        return indexMapper.toDto(savedEntity);
    }
}