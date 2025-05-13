package searchengine.services.test;

import searchengine.config.Site;

public interface SiteIndexingService<T> extends IndexingService<T>{
    void start(Site site);
}