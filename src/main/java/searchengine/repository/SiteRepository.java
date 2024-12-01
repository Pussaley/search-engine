package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    Optional<SiteEntity> findByUrl(String url);
    boolean existsByUrl(String url);
    @Modifying
    @Query("DELETE SiteEntity WHERE url = :url")
    void deleteByUrl(String url);
}