package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.entity.PageDTO;
import searchengine.dto.entity.SiteDTO;
import searchengine.services.impl.PageServiceImpl;
import searchengine.services.impl.SiteServiceImpl;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceConnector {
    private final SiteServiceImpl siteService;
    private final PageServiceImpl pageService;

    public Optional<PageDTO> findPageByPath(String path) {
        return pageService
                .findByPath(path);
    }

    public Optional<SiteDTO> findSiteByUrl(String siteUrl) {
        return siteService
                .findByUrl(siteUrl);
    }

    public boolean deleteSite(SiteDTO siteDTO) {
        return siteService.deleteSite(siteDTO);
    }
}