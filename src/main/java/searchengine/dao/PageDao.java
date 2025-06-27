package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.dto.entity.PageDto;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.PageEntity;
import searchengine.repository.PageRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PageDao {
    private final PageMapper pageMapper;
    private final PageRepository pageRepository;

    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id)
                .map(pageMapper::toDto);
    }

    public Optional<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path)
                .map(pageMapper::toDto);
    }

    public List<PageDto> findBySiteId(Long siteId) {
        return pageRepository.findAllBySiteId(siteId).stream()
                .map(pageMapper::toDto)
                .collect(Collectors.toList());
    }

    public PageDto save(PageDto pageDTO) {

        log.info("[{}] Saving the page '{}'", this.getClass().getSimpleName(), pageDTO.getPath());

        PageEntity entity = pageMapper.toEntity(pageDTO);
        PageEntity savedEntity = pageRepository.saveAndFlush(entity);
        return pageMapper.toDto(savedEntity);
    }
}