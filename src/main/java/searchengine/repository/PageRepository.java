package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.PageEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    @Query(
            nativeQuery = true,
            value = "select * from pages p where p.path = ?1 and p.site_id = ?2"
    )
    Optional<PageEntity> findByPathAndSiteId(String path, Long siteId);
}