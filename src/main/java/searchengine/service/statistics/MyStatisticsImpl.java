package searchengine.service.statistics;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.dto.statistics.DetailedStatisticsItem;
import searchengine.model.dto.statistics.StatisticsData;
import searchengine.model.dto.statistics.StatisticsResponse;
import searchengine.model.dto.statistics.TotalStatistics;
import searchengine.model.entity.SiteEntity;
import searchengine.service.impl.IndexingServiceImpl;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Primary
public class MyStatisticsImpl implements StatisticsService {

    @PersistenceContext
    private final EntityManager entityManager;
    private final IndexingServiceImpl indexingService;

    @Override
    @Transactional
    public StatisticsResponse getStatistics() {
        StatisticsResponse response = new StatisticsResponse();

        response.setStatistics(formStatisticsData());
        response.setResult(true);

        return response;
    }

    private List<DetailedStatisticsItem> formDetailedItemsList() {
        List<SiteEntity> fromSiteEntity = entityManager
                .createQuery("select s from SiteEntity as s", SiteEntity.class)
                .getResultList();

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for (SiteEntity site : fromSiteEntity) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());
            item.setError(site.getLastError());
            item.setStatus(site.getSiteStatus().toString());
            item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.UTC));

            Long pagesCount = (Long) entityManager.createNativeQuery("select count(*) from pages as p where p.site_id = ?")
                    .setParameter(1, site.getId())
                    .getSingleResult();

            Long lemmasCount = (Long) entityManager.createNativeQuery("select count(*) from lemmas as l where l.site_id = ?")
                    .setParameter(1, site.getId())
                    .getSingleResult();

            item.setPages(pagesCount.intValue());
            item.setLemmas(lemmasCount.intValue());

            detailed.add(item);
        }

        return detailed;
    }

    private StatisticsData formStatisticsData() {
        StatisticsData data = new StatisticsData();

        data.setTotal(formTotalStatistics());
        data.setDetailed(formDetailedItemsList());

        return data;
    }

    private TotalStatistics formTotalStatistics() {
        TotalStatistics total = new TotalStatistics();

        Long sites = (Long) entityManager
                .createNativeQuery("select count(*) from sites").getSingleResult();
        Long pages = (Long) entityManager
                .createNativeQuery("select count(*) from pages").getSingleResult();
        Long lemmas = (Long) entityManager
                .createNativeQuery("select count(*) from lemmas").getSingleResult();

        total.setSites(sites.intValue());
        total.setPages(pages.intValue());
        total.setLemmas(lemmas.intValue());
        total.setIndexing(indexingService.getIndexingIsRunning().get());

        return total;
    }
}