package searchengine.services;

import searchengine.dto.entity.SiteDTO;
import searchengine.model.SiteEntity;

import java.util.Optional;

public interface SiteService extends Service {
    Optional<SiteEntity> findByUrl(String url);
    SiteDTO save(SiteDTO siteDTO);
}
