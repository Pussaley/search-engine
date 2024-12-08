package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.SiteDTO;
import searchengine.mappers.SiteMapper;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.services.SiteService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;

    @Override
    public Optional<SiteEntity> findByUrl(String url) {
        return siteRepository.findByUrl(url);
    }

    @Override
    public SiteDTO save(SiteDTO siteDTO) {
        SiteEntity entity = siteMapper.toEntity(siteDTO);
        return siteMapper.toDTO(siteRepository.save(entity));
    }
}