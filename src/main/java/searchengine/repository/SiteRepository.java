package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteStatus;
import searchengine.model.entity.SiteEntity;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    @Query(
            nativeQuery = true,
            value = "select * from sites as s where s.name = ?"
    )
    Optional<SiteEntity> findByName(String siteName);

    @Modifying
    @Query("update SiteEntity as s set s.statusTime = now() where s.id = :siteId")
    void updateStatusTimeBySiteId(Long siteId);

    @Modifying
    @Query("update SiteEntity as s set s.siteStatus = :status, s.statusTime = now() where s.id = :siteId")
    void updateSiteStatusBySiteId(Long siteId, SiteStatus status);

    @Modifying
    @Query("update SiteEntity as s set s.siteStatus = :status, s.statusTime = now(), s.lastError = :error where s.id = :siteId")
    void updateSiteStatusWithErrorBySiteId(Long siteId, SiteStatus status, String error);
}