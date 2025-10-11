package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    @Query(
            nativeQuery = true,
            value = "select * from sites as s where s.name = ?"
    )
    Optional<SiteEntity> findByName(String siteName);

    @Query(
            nativeQuery = true,
            value = "select * from sites s where s.status != 'INDEXED' and last_error is NULL"
    )
    List<SiteEntity> findSitesByStatusNotIndexed();

    @Modifying
    @Query("update SiteEntity as s set s.statusTime = now() where s.id = :siteId")
    void updateStatusTimeBySiteId(Long siteId);
}