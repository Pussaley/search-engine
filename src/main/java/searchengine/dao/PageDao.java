package searchengine.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.entity.PageDTO;
import searchengine.mappers.PageMapper;
import searchengine.model.Page;
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

    public Optional<PageDTO> findById(Long id) {
        return pageRepository.findById(id)
                .map(pageMapper::toDTO);
    }

    public Optional<PageDTO> findByPath(String path) {
        return pageRepository.findByPath(path)
                .map(pageMapper::toDTO);
    }

    public List<PageDTO> findBySiteId(Long siteId) {
        return pageRepository.findAllBySiteId(siteId).stream()
                .map(pageMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PageDTO save(PageDTO pageDTO) {

        log.info("[{}] Saving the page '{}'", this.getClass().getSimpleName(), pageDTO.getPath());

        Page entity = pageMapper.toEntity(pageDTO);
        Page savedEntity = pageRepository.saveAndFlush(entity);
        return pageMapper.toDTO(savedEntity);
    }
}