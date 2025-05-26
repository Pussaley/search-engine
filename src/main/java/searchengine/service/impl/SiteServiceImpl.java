package searchengine.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.mapper.SiteMapper;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.model.entity.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.service.CRUDService;
import searchengine.util.url.URLUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl implements CRUDService<SiteDto> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;
    @Lazy
    @Autowired
    private PageServiceImpl pageService;

    @Override
    public Optional<SiteDto> findById(Long id) {
        return this.siteRepository.findById(id)
                .map(siteMapper::toDTO);
    }

    public Optional<SiteDto> findByUrlContaining(String url) {
        return this.siteRepository
                .findSiteEntitiesByUrlContaining(url)
                .map(siteMapper::toDTO);
    }

    public Optional<SiteDto> findByNameContaining(String name) {
        Optional<SiteDto> siteDTO = this.siteRepository
                .findSiteEntitiesByNameContaining(name)
                .map(siteMapper::toDTO);

        return siteDTO;
    }

    public SiteDto save(SiteDto siteDTO) {

        SiteEntity result = this.siteRepository
                .findByName(siteDTO.getName())
                .orElseGet(
                        () -> {
                            SiteEntity entity = siteMapper.toEntity(siteDTO);
                            return this.siteRepository.save(entity);
                        }
                );

        return siteMapper.toDTO(result);

    }

    public SiteDto updateSite(SiteDto siteDTO) {
        SiteDto dto;
        Optional<SiteEntity> foundSiteEntity = siteRepository.findSiteEntitiesByUrlContaining(siteDTO.getUrl());
        if (foundSiteEntity.isPresent()) {
            Long id = foundSiteEntity.get().getId();
            SiteEntity reference = siteRepository.getReferenceById(id);
            reference.setSiteStatus(siteDTO.getSiteStatus());

            SiteEntity saved = siteRepository.save(reference);
            dto = siteMapper.toDTO(saved);
        } else {
            dto = this.save(siteDTO);
        }

        return dto;
    }

    @Override
    public boolean deleteById(Long id) {
        this.siteRepository.deleteById(id);
        return this.siteRepository.existsById(id);
    }

    public void clearDatabaseBySiteName(String siteName) {
        siteRepository
                .findByName(siteName)
                .map(SiteEntity::getId)
                .stream()
                .findFirst()
                .ifPresent(
                        (siteId) -> {
                            pageService.deletePagesBySiteId(siteId);
                            siteRepository.deleteByName(siteName);
                        });
    }

    public void clearDatabaseBySiteId(Long id) {
        siteRepository
                .findById(id)
                .map(SiteEntity::getId)
                .ifPresent(
                        (siteId) -> {
                            pageService.deletePagesBySiteId(siteId);
                            siteRepository.deleteById(id);
                        });
    }

    public SiteDto createSiteEntityFromJsoupResponse(Connection.Response response) {

        URL responseUrl = response.url();
        String responseUrlString = response.url().toString();
        String hostName = responseUrl.getHost();

        int hostLength = hostName.split("\\.").length;

        String url = URLUtils.parseRootURL(URLUtils.removeEndBackslash(responseUrlString));
        String name = hostLength > 1
                ? hostName.split("\\.")[hostLength - 2]
                : hostName;

        return SiteDto.builder()
                .siteStatus(SiteStatus.INDEXING)
                .name(name)
                .url(url)
                .statusTime(LocalDateTime.now())
                .build();
    }

    public void clearSitesTableInDatabase(Collection<Site> sites) {
        sites.stream()
                .map(Site::getName)
                .forEach(this::clearDatabaseBySiteName);
    }
}