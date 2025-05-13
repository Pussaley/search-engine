package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.dto.entity.PageDTO;
import searchengine.model.Page;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    Optional<Page> findByPath(String path);
    List<Page> findAllBySiteId(Long siteId);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from pages as p where p.site_id = ?")
    void deletePagesBySiteId(Long id);
}