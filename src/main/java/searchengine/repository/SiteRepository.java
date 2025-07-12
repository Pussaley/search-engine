package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteStatus;
import searchengine.model.entity.SiteEntity;

import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {

    @Query(
            nativeQuery = true,
            value = "select * from sites as s where s.url = ?"
    )
    Optional<SiteEntity> findByUrl(String url);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from sites as s where s.url = ?")
    void deleteSiteByUrl(String url);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from sites as s where s.name = ?")
    void deleteByName(String name);

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
    @Query(
            nativeQuery = true,
            value = "update sites s set s.status_time = ? where s.id = ?"
    )
    void updateStatusTimeById(Temporal time, Long id);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "update sites s set s.status = ?2, s.status_time = ?3 where s.status = ?1"
    )
    void updateAllSitesSiteStatus(SiteStatus oldStatus, SiteStatus newStatus, Temporal time);
}