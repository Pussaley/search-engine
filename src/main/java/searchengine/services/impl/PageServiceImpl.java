package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dao.PageDao;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.mappers.PageMapper;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.services.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageServiceImpl implements CRUDService<PageDTO> {

    private final PageDao pageDao;
    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    private final SiteServiceImpl siteService;

    @Override
    public Optional<PageDTO> findById(Long id) {
        return pageRepository.findById(id)
                .map(pageMapper::toDTO);
    }

    @Override
    public boolean deleteById(Long id) {
        pageRepository.deleteById(id);
        return !pageRepository.existsById(id);
    }

    public PageDTO save(PageDTO pageDTO) {
        Page entity = pageMapper.toEntity(pageDTO);

        Optional<Page> optional = pageRepository.findByPath(entity.getPath());
        if ( optional.isPresent() ) {
            return pageDTO;
        }

        return pageMapper.toDTO(pageRepository.save(entity));
    }

    public void deletePagesBySiteId(Long id) {
        pageRepository.deletePagesBySiteId(id);
        pageRepository.flush();
    }

    public void createPageDtoFromResponse(Connection.Response response) {
        log.info("Saving the page: {}", response.url().toString());

        SiteDTO siteDTO = siteService.findByNameContaining(response.url().getHost().split("\\.")[0]).orElseThrow();

        PageDTO builtPageDto = PageDTO.builder()
                .code(response.statusCode())
                .path(response.url().getPath())
                .site(siteDTO)
                .content("content")
                .build();

        pageDao.save(builtPageDto);
    }
}