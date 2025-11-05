package searchengine.service.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.dto.response.search.PageWithRelevanceResponse;
import searchengine.model.dto.search.RelevancingPageAbs;
import searchengine.model.dto.search.RelevancingPageRel;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.DataSearchDAO;
import searchengine.util.morphology.LemmaFinder;
import searchengine.util.morphology.SnippetGenerator;
import searchengine.util.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSearchService {
    private final DataSearchDAO searchDAO;
    private final LemmaFinder lemmaFinder;
    private final SitesList sites;
    private final TextUtils textUtils;
    private final SnippetGenerator snippetGenerator;
    private final double MAX_LEMMA_FREQUENCY_PERCENTAGE = 0.75;

    public Map<Site, List<PageWithRelevanceResponse>> searchAsMap(String query, String siteUrl) {
        return sites.getSites().stream()
                .filter(site -> site.getUrl().equalsIgnoreCase(siteUrl))
                .findFirst().stream()
                .collect(Collectors.toMap(Function.identity(),
                        (site) -> findRelevancedPages(query, site.getUrl())));
    }

    public List<PageWithRelevanceResponse> findRelevancedPages(String query, String siteUrl) {

        List<String> lemmas = getLemmasFromQuery(query);
        Long siteId = searchDAO.getSiteIdBySiteUrl(siteUrl);
        Integer pagesCount = searchDAO.countPagesBySiteId(siteId);

        List<LemmaDto> list = findLemmaDtosSortedByFrequency(siteId, lemmas, pagesCount);

        if (list.isEmpty()) return Collections.emptyList();

        Iterator<LemmaDto> it = list.iterator();
        List<PageDto> pages = searchDAO.findPagesByLemmaId(it.next().getId());

        while (it.hasNext())
            pages = searchDAO.findPagesByLemmaIdInPages(it.next().getId(), pages.stream().map(PageDto::getId).toList());

        return pages.isEmpty() ? Collections.emptyList() : convertPagesIntoRelevancedPages(query, pages, lemmas);
    }

    private List<PageWithRelevanceResponse> convertPagesIntoRelevancedPages(String query, List<PageDto> pages, List<String> lemmas) {
        Map<PageDto, List<IndexDto>> data = pages.stream().collect(Collectors.toMap(
                Function.identity(),
                pageDto -> searchDAO.findIndexesByPageIdAndLemmas(pageDto.getId(), lemmas)
        ));

        List<RelevancingPageRel> list = new RelevancedPageConverter(data, query).get();
        return list.stream()
                .map(PageWithRelevanceResponse::new)
                .toList();
    }

    class RelevancedPageConverter {

        private final List<RelevancingPageAbs> list = new ArrayList<>();
        private final String query;

        RelevancedPageConverter(Map<PageDto, List<IndexDto>> data, String query) {
            this.query = query;
            calculate(data);
        }

        List<RelevancingPageRel> get() {
            return list.stream()
                    .map(page -> new RelevancingPageRel(
                            page,
                            MaxRelevance.getValue()))
                    .toList();
        }

        private void calculate(Map<PageDto, List<IndexDto>> data) {
            for (Map.Entry<PageDto, List<IndexDto>> entry : data.entrySet()) {
                PageDto pageDto = entry.getKey();
                List<IndexDto> indexes = entry.getValue();

                float absRelevance = indexes.stream().map(IndexDto::getRank).reduce((float) 0, Float::sum);
                list.add(new RelevancingPageAbs(
                        pageDto.getPath(),
                        textUtils.formTitle(pageDto.getContent()),
                        snippetGenerator.generateSnippet(pageDto.getContent(), query),
                        absRelevance));
                MaxRelevance.save(absRelevance);
            }
        }

        private static class MaxRelevance {
            @Getter
            private static float value = 0;

            public static void save(float newVal) {
                value = Math.max(value, newVal);
            }
        }
    }

    private List<LemmaDto> findLemmaDtosSortedByFrequency(Long siteId, List<String> lemmas, Integer pagesCount) {
        List<LemmaDto> lemmasOrderedByFrequency =
                searchDAO.findLemmasOrderByFrequency(siteId, lemmas);

        List<LemmaDto> result = lemmasOrderedByFrequency.stream()
                .filter(l -> checkLemmaFrequencyLessThreshold(l, pagesCount))
                .toList();

        return result.size() == lemmas.size() ? result : Collections.emptyList();
    }

    private boolean checkLemmaFrequencyLessThreshold(LemmaDto lemmaDto, int pagesCount) {
        return lemmaDto.getFrequency() < pagesCount * MAX_LEMMA_FREQUENCY_PERCENTAGE;
    }

    private List<String> getLemmasFromQuery(String query) {
        return Arrays.stream(textUtils.arrayContainsRussianWords(query))
                .filter(lemmaFinder::isNormalBaseWord)
                .map(lemmaFinder::getMorphLemma)
                .toList();
    }
}