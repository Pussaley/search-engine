package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PageServiceImpl implements CRUDService<PageDto> {

    private final PageRepository pageRepository;
    private final PageMapper pageMapper;
    @Lazy
    @Autowired
    private SiteServiceImpl siteService;

    @Override
    public Optional<PageDto> findById(Long id) {
        return pageRepository.findById(id).map(pageMapper::toDto);
    }

    public synchronized Optional<PageDto> findByPath(String path) {
        return pageRepository.findByPath(path).map(pageMapper::toDto);
    }

    @Override
    public boolean deleteById(Long id) {
        pageRepository.deleteById(id);
        return !pageRepository.existsById(id);
    }

    public synchronized PageDto save(PageDto pageDTO) {
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

    public PageDto createDtoFromJsoupResponse(Connection.Response response) throws IOException, SiteNotFoundException {

        URL responseUrl = response.url();

        String siteName = responseUrl.getHost();
        String sitePath = responseUrl.getPath();
        int statusCode = response.statusCode();

        String pageRawPath = responseUrl.getPath().concat(Objects.isNull(responseUrl.getQuery()) ? "" : responseUrl.toString().split(responseUrl.getPath())[1]);

        SiteDto siteDto = siteService.findByUrlContaining(siteName).orElseThrow(() -> {
            String site = responseUrl.getProtocol().concat("://").concat(responseUrl.getHost());
            return new SiteNotFoundException(site);
        });

        Document document = response.parse();
        String content = document.html();

        return PageDto.builder()
                .site(siteDto)
                .content(content)
                .path(pageRawPath)
                .code(statusCode)
                .build();
    }

    public Optional<PageDto> findByPathAndSiteUrl(String pagePath, String siteUrl) {
        return this.pageRepository.findByPathAndSiteUrl(pagePath, siteUrl).map(pageMapper::toDto);
    }

    public Integer getCount() {
        return pageRepository.getCount();
    }
}