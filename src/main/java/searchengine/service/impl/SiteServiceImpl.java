package searchengine.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.SiteMapper;
import searchengine.model.SiteStatus;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.dto.SiteDto;
import searchengine.repository.SiteRepository;
import searchengine.service.CRUDService;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SiteServiceImpl implements CRUDService<SiteDto, Long> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;
    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public Optional<SiteDto> findById(Long id) {
        return this.siteRepository.findById(id)
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

    public void update(SiteDto siteDto) {
        if (Objects.isNull(siteDto.getId()))
            throw new NullPointerException("The field <id> is empty");

        this.siteRepository.findById(siteDto.getId())
                .ifPresentOrElse(entity -> entityManager.merge(siteMapper.toEntity(siteDto)),
                        () -> log.error("Updating failed, reason: id {} not existed", siteDto.getId()));
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
    public void deleteById(Long id) {
        this.siteRepository.deleteById(id);
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
        log.info("Очищение базы успешно завершено");
    }

    public void updateStatusTimeById(Long id) {
        siteRepository.updateStatusTimeById(LocalDateTime.now(), id);
    }
}