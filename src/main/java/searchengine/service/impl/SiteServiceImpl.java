package searchengine.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.mapper.SiteMapper;
import searchengine.model.SiteStatus;
import searchengine.model.entity.dto.SiteDto;
import searchengine.model.entity.SiteEntity;
import searchengine.repository.SiteRepository;
import searchengine.service.CRUDService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl implements CRUDService<SiteDto> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;
    private final EntityManager entityManager;

    @Override
    public Optional<SiteDto> findById(Long id) {
        return this.siteRepository.findById(id)
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

    public SiteDto save(Site site) {
        return save(SiteDto.builder()
                .url(site.getUrl())
                .name(site.getName())
                .statusTime(LocalDateTime.now())
                .siteStatus(SiteStatus.INDEXING).build());
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

    public SiteDto update(SiteDto siteDto) {
        if (siteDto.getId() == null)
            throw new NullPointerException("The field <id> is empty");

        Optional<SiteEntity> found = this.siteRepository.findById(siteDto.getId());
        if (found.isPresent()) {

            SiteEntity entity = siteMapper.toEntity(siteDto);
            return siteMapper.toDTO(siteRepository.save(entity));
        }
        return this.save(siteDto);
    }

    public void updateNotIndexedEntitiesAfterStoppingIndexing(SiteStatus status, CharSequence message) {
        log.info("Обновляем статус сущностей");
        Iterator<SiteDto> it = this.findNotIndexedSites().iterator();
        while (it.hasNext()) {
            SiteDto dto = it.next();
            dto.setSiteStatus(status);
            dto.setLastError(message.toString());
            this.update(dto);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        this.siteRepository.deleteById(id);
        return this.siteRepository.existsById(id);
    }

    public void clearDatabaseBySiteName(String siteName) {
        siteRepository.findByName(siteName).ifPresent(
                entity -> {
                    Long id = entity.getId();
                    entityManager.createQuery("DELETE FROM LemmaEntity WHERE site = :site")
                            .setParameter("site", entity)
                            .executeUpdate();
                    siteRepository.deleteById(id);
                }
        );
    }

    public void updateStatusTimeById(Long id) {
        siteRepository.updateStatusTimeById(LocalDateTime.now(), id);
    }
}