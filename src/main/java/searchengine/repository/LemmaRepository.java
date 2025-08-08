package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.LemmaEntity;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    @Query(
            nativeQuery = true,
            value = "select * from lemmas as l where l.lemma = ? and  l.site_id = ?")
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, Long id);

    @Modifying
    @Query(
            nativeQuery = true,
            value = "insert into lemmas (lemma, site_id, frequency) VALUES (?, ?, 1) on duplicate key update frequency = frequency + 1"
    )
    void insertLemmaOrUpdateFrequency(String lemma, Long siteId);
}