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

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final LemmaServiceImpl lemmaService;
    private final IndexServiceImpl indexService;

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

    public Optional<SiteDto> findByUrl(String url) {
        return this.siteRepository
                .findByUrl(url)
                .map(siteMapper::toDTO);
    }

    public Optional<SiteDto> findByName(String name) {
        return this.siteRepository
                .findByName(name)
                .map(siteMapper::toDTO);
    }

    public List<SiteDto> findNotIndexedSites() {
        return this.siteRepository.findNotIndexedEntities()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(siteMapper::toDTO)
                .collect(Collectors.toList());
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

    public SiteDto updateSite(SiteDto siteDto) {
        if (siteDto.getId() == null)
            throw new NullPointerException("The field <id> is empty");

        Optional<SiteEntity> found = this.siteRepository.findById(siteDto.getId());
        if (found.isPresent()) {

            SiteEntity entity = siteMapper.toEntity(siteDto);
            return siteMapper.toDTO(siteRepository.save(entity));
        }
        return this.save(siteDto);
    }

    @Deprecated
    public SiteDto updateSiteDepr(SiteDto siteDTO) {
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

    public void updateNotIndexedEntitiesAfterStoppingIndexing(SiteStatus status, CharSequence message) {
        log.info("Обновляем статус сущностей");
        Iterator<SiteDto> it = this.findNotIndexedSites().iterator();
        while (it.hasNext()) {
            SiteDto dto = it.next();
            dto.setSiteStatus(status);
            dto.setLastError(message.toString());
            this.updateSite(dto);
        }
    }

    public SiteDto save(Site site) {
        return save(SiteDto.builder().url(site.getUrl()).name(site.getName()).statusTime(LocalDateTime.now()).siteStatus(SiteStatus.INDEXING).build());
    }

    public SiteDto createSiteEntityFromJsoupResponse(Connection.Response response) {

        URL responseUrl = response.url();

        String siteName = responseUrl.getHost();
        String siteUrl = responseUrl.getProtocol().concat("://").concat(responseUrl.getHost());

        return SiteDto.builder()
                .siteStatus(SiteStatus.INDEXING)
                .name(siteName)
                .url(siteUrl)
                .statusTime(LocalDateTime.now())
                .build();
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
                            siteRepository.deleteById(siteId);
                            lemmaService.deleteLemmasBySiteId(siteId);
                            indexService.deleteIndexesBySiteId(siteId);
                            pageService.deletePagesBySiteId(siteId);
                        });
    }

    public void updateStatusTimeById(Long id) {
        siteRepository.updateStatusTimeById(LocalDateTime.now(), id);
    }
}