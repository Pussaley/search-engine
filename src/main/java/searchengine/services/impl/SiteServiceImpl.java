package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.dto.entity.SiteDTO;
import searchengine.exceptions.SiteNotFoundException;
import searchengine.mappers.SiteMapper;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.services.CRUDService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl implements CRUDService<SiteDTO> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;

    @Override
    public Optional<SiteDTO> findById(Long id) {
        return this.siteRepository.findById(id)
                .map(siteMapper::toDTO);
    }

    public Optional<SiteDTO> findByUrl(String url) {
        return this.findByUrlWithoutMapping(url)
                .map(siteMapper::toDTO);
    }

    private Optional<SiteEntity> findByUrlWithoutMapping(String url) {
        return this.siteRepository.findByUrl(url);
    }

    public Optional<SiteDTO> findByName(String name) {
        return this.siteRepository
                .findByName(name)
                .map(siteMapper::toDTO);
    }

    private Optional<SiteEntity> findByNameWithoutMapping(String name) {
        return this.siteRepository.findByName(name);
    }

    public Optional<SiteDTO> findByUrlContaining(String url) {
        return this.siteRepository
                .findSiteEntitiesByUrlContaining(url)
                .map(siteMapper::toDTO);
    }

    public Optional<SiteDTO> findByNameContaining(String name) {
        Optional<SiteDTO> siteDTO = this.siteRepository
                .findSiteEntitiesByNameContaining(name)
                .map(siteMapper::toDTO);

        return siteDTO;
    }

    public SiteDTO save(SiteDTO siteDTO) {

        SiteEntity entity = siteMapper.toEntity(siteDTO);
        SiteEntity saved = siteRepository.saveAndFlush(entity);

        return siteMapper.toDTO(saved);
    }

    public SiteDTO updateSite(SiteDTO siteDTO) {
        SiteDTO dto;
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

    public void delete(Site site) {

        String siteUrl = site.getUrl();
        String siteName = site.getName();

        SiteEntity entity = this.siteRepository.findByName(siteName)
                .orElseGet(
                        () -> this.siteRepository.findByUrl(siteUrl)
                                .orElseThrow(SiteNotFoundException::new));

        this.delete(entity);
    }

    public void delete(SiteEntity entity) {
        this.siteRepository.delete(entity);
        this.siteRepository.flush();
    }

    @Override
    public boolean deleteById(Long id) {
        this.siteRepository.deleteById(id);
        return this.siteRepository.existsById(id);
    }
}