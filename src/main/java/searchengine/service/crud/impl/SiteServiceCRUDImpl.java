package searchengine.service.crud.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.mapper.SiteMapper;
import searchengine.model.SiteStatus;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.dto.SiteDto;
import searchengine.repository.SiteRepository;
import searchengine.service.crud.CRUDService;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(timeout = 15)
public class SiteServiceCRUDImpl implements CRUDService<SiteDto> {

    private final SiteRepository siteRepository;
    private final SiteMapper siteMapper;
    @PersistenceContext
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public Optional<SiteDto> findById(Long id) {
        return this.siteRepository.findById(id)
                .map(siteMapper::toDTO);
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

    public synchronized SiteDto update(SiteDto siteDto) {
        if (Objects.isNull(siteDto.getId()))
            throw new NullPointerException("Поле id не может быть пустым");

        return siteMapper.toDTO(siteRepository.save(siteMapper.toEntity(siteDto)));

    }

    @Override
    public void deleteById(Long id) {
        this.siteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<SiteDto> findByName(String name) {
        return siteRepository.findByName(name).map(siteMapper::toDTO);
    }

    public void updateSiteStatus(Long id, SiteStatus siteStatus) {
        entityManager.createNativeQuery("update sites as s set s.status = ?, s.status_time = ? where s.id = ?")
                .setParameter(1, siteStatus.toString())
                .setParameter(2, LocalDateTime.now())
                .setParameter(3, id)
                .executeUpdate();
    }

    public void clearDatabaseFromSitePageLemmaIndexEntities(String siteName) {
        findByName(siteName).ifPresent(dto -> {
            Long siteId = dto.getId();
            entityManager.createQuery("delete from IndexEntity as i where i.page.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            entityManager.createQuery("delete from PageEntity as p where p.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            entityManager.createQuery("delete from LemmaEntity as l where l.site.id = :siteId")
                    .setParameter("siteId", siteId)
                    .executeUpdate();

            deleteById(siteId);
        });
    }

    public synchronized void updateSiteStatusWithError(Long siteId, SiteStatus status, String error) {
        entityManager.createNativeQuery("update sites as s set s.status = ?, s.status_time = now(), s.last_error = ? where s.id = ?")
                .setParameter(1, status )
                .setParameter(2, error)
                .setParameter(3, siteId)
                .executeUpdate();
    }

    public void updateStatusTime(Long siteId) {
        siteRepository.updateStatusTimeBySiteId(siteId);
    }
}