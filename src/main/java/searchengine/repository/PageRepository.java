package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.PageEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    Optional<PageEntity> findByPath(String path);
    List<PageEntity> findAllBySiteId(Long siteId);
    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from pages as p where p.site_id = ?")
    void deletePagesBySiteId(Long id);

    @Query(
            nativeQuery = true,
            value = "select * from pages as p where p.path = ? and p.site_id = (select s.id from sites as s where s.url = ?);"
    )
    Optional<PageEntity> findByPathAndSiteUrl(String path, String siteUrl);
}