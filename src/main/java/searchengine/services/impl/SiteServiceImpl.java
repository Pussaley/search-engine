package searchengine.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.dto.entity.SiteDTO;
import searchengine.mappers.MySiteMapper;
import searchengine.mappers.SiteMapper;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;

import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;
    private final MySiteMapper mySiteMapper;
    private final EntityManager entityManager;

    public Optional<SiteDTO> findByUrl(String url) {
        return siteRepository.findByUrl(url)
                .map(siteMapper::toDTO);
    }

    public SiteDTO save(SiteDTO siteDTO) {
        SiteEntity entity = mySiteMapper.toEntity(siteDTO);
        SiteEntity saved = siteRepository.save(entity);
        entityManager.flush();

        SiteEntity foundEntity = entityManager.find(SiteEntity.class, 1);

        return mySiteMapper.toDTO(foundEntity);
    }

    public void deleteSite(SiteDTO siteDTO) {
        SiteEntity entity = siteMapper.toEntity(siteDTO);
        siteRepository.delete(entity);
    }
}