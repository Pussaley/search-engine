package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.LemmaEntityMapper;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.LemmaEntity;
import searchengine.repository.LemmaRepository;
import searchengine.service.CRUDService;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class LemmaServiceImpl implements CRUDService<LemmaDto> {

    private final LemmaRepository lemmaRepository;
    private final LemmaEntityMapper lemmaMapper;

    @Override
    public Optional<LemmaDto> findById(Long id) {
        return lemmaRepository.findById(id).map(lemmaMapper::toDto);
    }

    public Optional<LemmaDto> findByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma)
                .map(lemmaMapper::toDto);
    }

    @Override
    public LemmaDto save(LemmaDto lemmaDto) {
        LemmaEntity entity = lemmaMapper.toEntity(lemmaDto);
        LemmaEntity saved = lemmaRepository.save(entity);

        return lemmaMapper.toDto(saved);
    }

    public LemmaDto update(LemmaDto lemmaDto) {
        return Objects.isNull(lemmaDto.getId()) ? lemmaDto : lemmaMapper.toDto(lemmaRepository.save(lemmaMapper.toEntity(lemmaDto)));
    }

    public void deleteLemmasBySiteId(Long siteId) {
        lemmaRepository.deleteLemmasBySiteId(siteId);
        lemmaRepository.flush();
    }

    @Override
    public boolean deleteById(Long id) {
        lemmaRepository.deleteById(id);
        return lemmaRepository.existsById(id);
    }
}