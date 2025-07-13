package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.CustomLemmaMapper;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.repository.LemmaRepository;
import searchengine.service.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LemmaServiceImpl implements CRUDService<LemmaDto> {

    private final LemmaRepository lemmaRepository;
    private final CustomLemmaMapper lemmaMapper;

    public LemmaEntity getReferenceById(Long lemmaId) {
        return lemmaRepository.getReferenceById(lemmaId);
    }

    @Override
    public Optional<LemmaDto> findById(Long id) {
        return lemmaRepository.findById(id)
                .map(lemmaMapper::toDto);
    }

    @Override
    public LemmaDto save(LemmaDto lemmaDto) {
        LemmaEntity entity = lemmaMapper.toEntity(lemmaDto);
        LemmaEntity saved = lemmaRepository.save(entity);

        return lemmaMapper.toDto(saved);
    }

    public Optional<LemmaDto> findByLemmaAndSiteId(String lemma, Long siteId) {
        return lemmaRepository.findByLemmaAndSiteId(lemma, siteId).map(lemmaMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
        lemmaRepository.deleteById(id);
    }
}