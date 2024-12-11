package searchengine.services;

import searchengine.dto.entity.PageDTO;
import searchengine.model.Page;

import java.util.Optional;

public interface PageService extends Service {
    boolean existsByPath(String path);
    PageDTO save(PageDTO pageDTO);
    Optional<PageDTO> findByPath(String path);
}