package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.LemmaEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {

    @Query(
            nativeQuery = true,
            value = "select * from lemmas as l where l.lemma = ?")
    List<LemmaEntity> findByLemma(String lemma);
    @Modifying
    @Query(
            nativeQuery = true,
            value = "delete from lemmas as l where l.site_id = ?")
    void deleteLemmasBySiteId(Long siteId);
}