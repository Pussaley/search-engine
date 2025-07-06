package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.LemmaEntityMapper;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.repository.LemmaRepository;
import searchengine.service.CRUDService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class LemmaServiceImpl implements CRUDService<LemmaDto, Long> {

    private final LemmaRepository lemmaRepository;
    private final LemmaEntityMapper lemmaMapper;

    @Override
    public Optional<LemmaDto> findById(Long id) {
        return lemmaRepository.findById(id)
                .map(lemmaMapper::toDto);
    }

    public List<LemmaDto> findByLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma).stream().map(lemmaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public LemmaDto save(LemmaDto lemmaDto) {
        LemmaEntity entity = lemmaMapper.toEntity(lemmaDto);
        LemmaEntity saved = lemmaRepository.save(entity);

        return lemmaMapper.toDto(saved);
    }

    public LemmaDto update(LemmaDto lemmaDto) {
        return lemmaMapper.toDto(lemmaRepository.save(lemmaMapper.toEntity(lemmaDto)));
    }

    public void deleteLemmasBySiteId(Long siteId) {
        lemmaRepository.deleteLemmasBySiteId(siteId);
        lemmaRepository.flush();
    }

    @Override
    public void deleteById(Long id) {
        lemmaRepository.deleteById(id);
    }
}