package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteEntity;
import searchengine.model.SiteStatus;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final SitesList sites;

    public void deleteTest() {
        List<String> collect = getSiteStream()
                .map(Site::getUrl)
                .toList();
        String url = collect.iterator().next();
        siteRepository.deleteByUrl(url);
    }

    @Override
    public void startIndexing() {

        deleteTest();
        deleteAllRecordsFromSiteRepository();
        createNewRecordsInSiteRepository();

        //TODO: обходить все страницы, начиная с главной, добавлять их адреса,
        // - статусы и содержимое в базу данных в таблицу page;

        //TODO: в процессе обхода постоянно обновлять дату и время в поле
        // - status_time таблицы site на текущее;
        // - по завершении обхода изменять статус (поле status) на INDEXED;
        // - если произошла ошибка и обход завершить не удалось, изменять
        // - статус на FAILED и вносить в поле last_error понятную
        // - информацию о произошедшей ошибке.
    }

    private void deleteAllRecordsFromSiteRepository() {
        //TODO:удалять все имеющиеся данные по этому сайту (записи из таблиц site и page);

        List<String> urlList = getSiteStream()
                .map(searchengine.config.Site::getUrl)
                .toList();

        urlList.forEach(url -> {
            if (siteRepository.existsByUrl(url)) {
                siteRepository.deleteByUrl(url);
                //siteRepository.flush();
            }
        });
    }

    private void createNewRecordsInSiteRepository() {
        //TODO:создавать в таблице site новую запись со статусом INDEXING
        getSiteStream()
                .forEach(element -> {

                            SiteEntity site = new SiteEntity();
                            site.setUrl(element.getUrl());
                            site.setName(element.getName());
                            site.setStatus(SiteStatus.INDEXING);
                            siteRepository.save(site);

                            siteRepository.findByUrl(site.getUrl()).ifPresent(
                                    opt -> {
                                        Page page = new Page();
                                        page.setSite(opt);
                                        page.setPath("/");
                                        page.setCode(200);
                                        page.setContent("<html>");

                                        pageRepository.save(page);
                                    }
                            );
                        }
                );
    }

    private Stream<searchengine.config.Site> getSiteStream() {
        return sites.getSites()
                .stream();
    }
}