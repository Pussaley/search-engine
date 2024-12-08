package searchengine.services;

import searchengine.dto.entity.PageDTO;

public interface PageService {
    boolean existsByPath(String path);
    PageDTO save(PageDTO pageDTO);
}