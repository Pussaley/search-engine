package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.exception.SiteNotFoundException;
import searchengine.mapper.PageMapper;
import searchengine.model.dto.entity.PageDto;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.entity.PageEntity;
import searchengine.repository.PageRepository;
import searchengine.service.CRUDService;

import java.io.IOException;
import java.net.URL;
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

    public Optional<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path)
                .map(pageMapper::toDTO);
    }

    @Override
    public boolean deleteById(Long id) {
        pageRepository.deleteById(id);
        return !pageRepository.existsById(id);
    }

    public PageDto save(PageDto pageDTO) {
        PageEntity saved = pageRepository.save(pageMapper.toEntity(pageDTO));
        return pageMapper.toDTO(saved);
    }

    public void deletePagesBySiteId(Long id) {
        pageRepository.deletePagesBySiteId(id);
        pageRepository.flush();
    }

    public PageDto createDtoFromJsoupResponse(Connection.Response response) throws IOException, SiteNotFoundException {

        URL responseUrl = response.url();

        String siteName = responseUrl.getHost();
        String sitePath = responseUrl.getPath();
        int statusCode = response.statusCode();

        SiteDto siteDto = siteService.findByUrlContaining(siteName)
                .orElseThrow(() -> {
                    String site = responseUrl.getProtocol().concat("://").concat(responseUrl.getHost());
                    return new SiteNotFoundException(site);
                });

        Document document = response.parse();
        String content = document.html();

        return PageDto.builder()
                .site(siteDto)
                .content(content)
                .path(sitePath)
                .code(statusCode)
                .build();
    }

    public Optional<PageDto> findByPathAndSiteUrl(String pagePath, String siteUrl) {
        return this.pageRepository.findByPathAndSiteUrl(pagePath, siteUrl)
                .map(pageMapper::toDTO);
    }
}