package searchengine.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.URLUtils;
import searchengine.dto.entity.SiteDTO;
import searchengine.mappers.SiteMapper;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl {

    private final SiteRepository siteRepository;
    private final PageServiceImpl pageService;
    private final SiteMapper siteMapper;

    public Optional<SiteDTO> findByUrl(String url) {
        return siteRepository.findByUrl(url)
                .map(siteMapper::toDTO);
    }

    public SiteDTO save(SiteDTO siteDTO) {
        SiteEntity entity = siteMapper.toEntity(siteDTO);
        SiteEntity savedEntity = siteRepository.saveAndFlush(entity);
        return siteMapper.toDTO(savedEntity);
    }

    public boolean deleteSite(SiteDTO siteDTO) {
        deleteSiteByUrl(siteDTO.getUrl());

        return true;
    }

    public void deleteSiteByUrl(String url) {
        siteRepository
                .findByUrl(url)
                .ifPresent(site -> {
                    Long siteId = site.getId();
                    pageService.deleteBySiteId(siteId);
                    siteRepository.deleteById(siteId);
                    siteRepository.flush();

                    log.info("--------------test---------------");
                    log.info("URL удаляемого сайта: {}", site.getUrl());
                    log.info("ID удаляемого сайта: {}", site.getId());
                    log.info("--------------test---------------");
                });
    }

    public List<SiteEntity> findAll() {
       return siteRepository.findAll();
    }

    public void update(SiteDTO siteDTO) {
        siteRepository
                .findByUrl(siteDTO.getUrl())
                .ifPresent(entity -> entity.setStatus(siteDTO.getSiteStatus()));
    }
}