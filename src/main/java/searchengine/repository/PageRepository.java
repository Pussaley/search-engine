package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    boolean existsByPath(String path);
    Optional<Page> findByPath(String path);
}