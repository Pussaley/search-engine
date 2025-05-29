package searchengine.service.impl.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteStatus;
import searchengine.model.dto.entity.SiteDto;
import searchengine.service.impl.SiteServiceImpl;
import searchengine.service.recursive.ForkJoinRecursiveTask;
import searchengine.service.recursive.ForkJoinRecursiveTaskDemo;
import searchengine.service.recursive.ForkJoinTaskStatus;
import searchengine.util.jsoup.JSOUPParser;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceStarter {

    private final JSOUPParser parser;
    private final SiteServiceImpl siteService;
    private final SitesList sitesList;
    private ForkJoinPool pool;
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public void startRecursiveIndexing(Collection<Site> sites) {
        siteService.clearSitesTableInDatabase(sites);

        isRunning.compareAndSet(false, true);

        pool = new ForkJoinPool();
        new Thread( () -> sites.forEach(
                (site) -> {

                    log.info("|{}| is starting the indexing of the site: {}", Thread.currentThread().getName(), site.getUrl());

                    String siteURL = site.getUrl();
                    String siteName = site.getName();

                    Connection.Response response = parser.execute(siteURL);

                    if (Objects.nonNull(response)) {

                        SiteDto siteDTO = SiteDto.builder()
                                .statusTime(LocalDateTime.now())
                                .url(siteURL)
                                .name(siteName)
                                .siteStatus(SiteStatus.INDEXING)
                                .build();

                        SiteDto savedDto = siteService.save(siteDTO);

                        ForkJoinRecursiveTask task = new ForkJoinRecursiveTask(response, parser.parseAbsoluteLinks(response));
//                        ForkJoinRecursiveTaskDemo task = new ForkJoinRecursiveTaskDemo(site);
                        Boolean resultOfRecursiveTask = pool.invoke(task);

                        SiteStatus status = resultOfRecursiveTask ? SiteStatus.INDEXED : SiteStatus.FAILED;

                        savedDto.setSiteStatus(status);
                        siteService.updateSite(savedDto);
                    }

                    log.info("|{}| is ending the indexing of the site: {}", Thread.currentThread().getName(), site.getUrl());
                }
        )).start();
        log.info("-------------------------------------------------");
    }

    public boolean stopIndexing() {
        new Thread(() -> {
            if (pool != null && isRunning.compareAndSet(true, false)) {

                int defaultTimeOut = 5;
                boolean isTerminated = false;
                try {
                    pool.shutdownNow();
                    log.info("Индексация была остановлена пользователем");
                    isTerminated = pool.awaitTermination(defaultTimeOut, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.info("Ожидание завершения FJP было прервано <{}>", e.getMessage());
                    Thread.currentThread().interrupt();
                }
//                return pool.isTerminated() && isTerminated;
            } else {
                log.info("Индексация по каким-то причинам не запущена, скорее всего завершилась");
//                return false;
            }
        }).start();

        return true;
    }
}