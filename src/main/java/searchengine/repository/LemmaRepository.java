package searchengine.repository;

import aj.org.objectweb.asm.commons.Remapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.dto.LemmaDto;

import java.util.List;
import java.util.Optional;

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

    @Query(
            nativeQuery = true,
            value = "select * from lemmas as l where l.lemma = ? and  l.site_id = ?")
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, Long id);
}