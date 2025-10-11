package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.dto.response.search.PageWithRelevanceResponse;
import searchengine.model.dto.search.PageWithRelevance;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.DataSearchDAO;
import searchengine.util.morphology.LemmaFinder;
import searchengine.util.morphology.SnippetGenerator;
import searchengine.util.text.TextUtils;

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
        List<PageWithRelevance> result = pages.stream()
                .map(page -> new PageWithRelevance(page, searchDAO.findIndexesByPageIdAndLemmas(page.getId(), lemmas)))
                .toList();

        return result.stream()
                .map(pageWithRelevance -> {
                    String content = pageWithRelevance.getPage().getContent();

                    return new PageWithRelevanceResponse(
                            pageWithRelevance.getPage().getPath(),
                            textUtils.formTitle(content),
                            snippetGenerator.generateSnippet(content, query),
                            pageWithRelevance.getRelRelevance());
                })
                .toList();
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