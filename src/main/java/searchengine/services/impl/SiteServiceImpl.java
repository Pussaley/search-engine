package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.SiteDTO;
import searchengine.mappers.SiteMapper;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteServiceImpl {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;

    public Optional<SiteDTO> findByUrl(String url) {
        return siteRepository.findByUrl(url)
                .map(siteMapper::toDTO);
    }

    public SiteDTO save(SiteDTO siteDTO) {
        SiteEntity entity = siteMapper.toEntity(siteDTO);
        SiteEntity savedEntity = siteRepository.save(entity);
        return siteMapper.toDTO(savedEntity);
    }

    public boolean deleteSite(SiteDTO siteDTO) {
        SiteEntity entity = siteMapper.toEntity(siteDTO);
        entity.clearPages();
        siteRepository.delete(entity);
        siteRepository.flush();
        return true;
    }
}