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
}