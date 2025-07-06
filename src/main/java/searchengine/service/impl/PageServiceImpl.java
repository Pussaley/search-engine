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

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageServiceImpl implements CRUDService<PageDto, Long> {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;

    @Override
    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id).map(pageMapper::toDto);
    }

    public Optional<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path).map(pageMapper::toDto);
    }

    @Override
    public void deleteById(Long id) {
        pageRepository.deleteById(id);
    }

    public PageDto save(PageDto pageDTO) {
        return findByPath(pageDTO.getPath()).orElseGet(() -> {
            PageEntity entity = pageMapper.toEntity(pageDTO);
            PageEntity saved = pageRepository.save(entity);
            pageRepository.flush();
            return pageMapper.toDto(saved);
        });
    }

    public void deletePagesBySiteId(Long id) {
        pageRepository.deletePagesBySiteId(id);
        pageRepository.flush();
    }

    public Optional<PageDto> findByPathAndSiteUrl(String pagePath, String siteUrl) {
        return this.pageRepository.findByPathAndSiteUrl(pagePath, siteUrl).map(pageMapper::toDto);
    }
}