package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}