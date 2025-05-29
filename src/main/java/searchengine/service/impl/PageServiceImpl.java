package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.mapper.PageMapper;
import searchengine.model.entity.PageEntity;
import searchengine.repository.PageRepository;
import searchengine.service.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageServiceImpl implements CRUDService<PageDto> {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final SiteServiceImpl siteService;

    @Override
    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id)
                .map(pageMapper::toDTO);
    }

    @Override
    public boolean deleteById(Long id) {
        pageRepository.deleteById(id);
        return !pageRepository.existsById(id);
    }

    public PageDto save(PageDto pageDTO) {

        Optional<PageEntity> optional = pageRepository.findByPath(pageDTO.getPath());
        if (optional.isPresent()) {
            log.info("{} is already saved in the database", optional.get().getPath());
            return pageDTO;
        }
        PageEntity saved = pageRepository.save(pageMapper.toEntity(pageDTO));

        return pageMapper.toDTO(saved);
    }

    public void deletePagesBySiteId(Long id) {
        pageRepository.deletePagesBySiteId(id);
        pageRepository.flush();
    }

    public PageDto createDtoFromJsoupResponse(Connection.Response response) {

        String siteName = response.url().getHost().split("\\.")[0];

        SiteDto siteDTO = siteService.findByNameContaining(siteName)
                .orElseGet(
                        () -> {
                            SiteDto savedSite = siteService.createSiteEntityFromJsoupResponse(response);

                            log.info("PageServiceImpl | Url: {}", savedSite.getUrl());
                            return siteService.save(savedSite);
                        }
                );

        return PageDto.builder()
                .code(response.statusCode())
                .path(response.url().getPath())
                .site(siteDTO)
                .content("content")
                .build();
    }
}