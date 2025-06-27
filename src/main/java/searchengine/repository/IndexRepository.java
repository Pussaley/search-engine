package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.IndexEntity;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Long> {

    @Query(
            nativeQuery = true,
            value = "select * from indexes as i where lemma_id = ? and page_id = ?")
    Optional<IndexEntity> findByLemmaIdAndPageId(Long lemmaId, Long pageId);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from indexes as i where i.page_id = ?")
    void deleteIndexesByPageId(Long pageId);
}