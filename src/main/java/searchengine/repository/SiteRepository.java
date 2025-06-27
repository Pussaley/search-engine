package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.SiteEntity;

import java.sql.Timestamp;
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

    Optional<SiteEntity> findSiteEntitiesByUrlContaining(String url);

    Optional<SiteEntity> findSiteEntitiesByNameContaining(String name);

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
            value = "select * from sites as s where s.status != 'INDEXED' and last_error is NULL"
    )
    List<Optional<SiteEntity>> findNotIndexedEntities();

    @Modifying
    @Query(
            nativeQuery = true,
            value = "update sites as s set s.status_time = ? where s.id = ?"
    )
    void updateStatusTimeById(Temporal time, Long id);
}