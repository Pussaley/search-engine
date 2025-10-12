package searchengine.service.morphology;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.model.entity.dto.SiteDto;
import searchengine.service.crud.impl.IndexServiceCRUDImpl;
import searchengine.service.crud.impl.LemmaServiceCRUDImpl;
import searchengine.util.morphology.LemmaFinder;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class LemmaProcessor {
    private final LemmaFinder lemmaFinder;
    private final LemmaServiceCRUDImpl lemmaService;
    private final IndexServiceCRUDImpl indexService;
    private static final Map<String, ReentrantLock> LEMMA_LOCKS = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    public void processLemmas(SiteDto site, PageDto page) {
        if (!isPositive(page.getCode()))
            return;

        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(page.getContent());
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemma = entry.getKey();

            ReentrantLock lemmaLock;
            synchronized (lock) {
                lemmaLock = LEMMA_LOCKS.computeIfAbsent(lemma, k -> new ReentrantLock());
            }

            try {
                lemmaLock.lockInterruptibly();
                Integer lemmaCount = entry.getValue();
                try {
                    LemmaDto lemmaDto = lemmaService.insertLemmaOrUpdateFrequency(lemma, site.getId());

                    indexService.findByPageAndLemma(page, lemmaDto).orElseGet(() ->
                            indexService.save(IndexDto.builder()
                                    .pageId(page.getId())
                                    .lemmaId(lemmaDto.getId())
                                    .rank(lemmaCount.floatValue())
                                    .build()));
                } finally {
                    lemmaLock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Индексация была прервана пользователем");
            }
        }
    }

    private boolean isPositive(int statusCode) {
        return statusCode / 100 == 2;
    }
}