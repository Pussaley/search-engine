package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.PageRepository;
import searchengine.service.CRUDService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageServiceImpl implements CRUDService<PageDto> {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;

    public PageEntity getReferenceById(Long pageId) {
        return pageRepository.getReferenceById(pageId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id).map(pageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path).stream().map(pageMapper::toDto).toList();
    }

    public Optional<PageDto> findByPathAndSiteId(String path, Long siteId) {
        return pageRepository.findByPathAndSiteId(path, siteId).map(pageMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
        pageRepository.deleteById(id);
    }

    public PageDto save(PageDto pageDTO) {
        return findByPath(pageDTO.getPath())
                .stream()
                .filter(el -> Objects.equals(el.getSite().getId(), pageDTO.getSite().getId()))
                .findFirst()
                .orElseGet(() -> {
                    PageEntity entity = pageMapper.toEntity(pageDTO);
                    PageEntity saved = pageRepository.save(entity);
                    pageRepository.flush();
                    return pageMapper.toDto(saved);
                });
    }
}