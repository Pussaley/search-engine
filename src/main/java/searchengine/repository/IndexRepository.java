package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.key.IndexEntityId;

import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, IndexEntityId> {

    @Query(
            nativeQuery = true,
            value = "select * from indexes as i where page_id = ? and lemma_id = ?")
    Optional<IndexEntity> findByPageIdAndLemmaId(Long pageId, Long lemmaId);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from indexes as i where i.page_id = ?")
    void deleteIndexesByPageId(Long pageId);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from indexes as i where lemma_id = ? and page_id = ?")
    void deleteByLemmaIdAndPageId(Long lemmaId, Long pageId);
}