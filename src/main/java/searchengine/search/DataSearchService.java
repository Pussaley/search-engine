package searchengine.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.search.dao.DataSearchDAO;
import searchengine.search.model.PageWithRelevanceResponse;
import searchengine.service.morphology.LemmaFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSearchService {
    private final DataSearchDAO dao;
    private final LemmaFinder lemmaFinder;
    private final SitesList sites;

    public Map<Site, List<PageWithRelevanceResponse>> searchTest(String query, String siteUrl, String offset, String limit) {
        return sites.getSites().stream()
                .filter(site -> site.getUrl().equalsIgnoreCase(siteUrl))
                .findFirst()
                .stream()
                .collect(Collectors.toMap(Function.identity(),
                        (site) -> search(query, site.getUrl(), offset, limit)));
    }

    public List<PageWithRelevanceResponse> search(String query, String siteUrl, String offset, String limit) {

        List<String> lemmas = getLemmasFromQuery(query);

        Long siteId = dao.getSiteIdBySiteUrl(siteUrl);
        List<LemmaDto> resultLemmas = new ArrayList<>();
        lemmas.forEach(l -> dao.findLemmasByLemmaAndSiteId(l, siteId).ifPresent(resultLemmas::add));

        if (resultLemmas.isEmpty()) return Collections.emptyList();

        Set<LemmaDto> sortedLemmas = new TreeSet<>(Comparator.comparing(LemmaDto::getFrequency));
        sortedLemmas.addAll(resultLemmas);

        LemmaDto[] array = sortedLemmas.toArray(LemmaDto[]::new);
        List<PageDto> pages = new ArrayList<>();
        for (int i = 0; i < array.length; i++) {
            LemmaDto lemmaDto = array[i];
            if (i == 0)
                pages.addAll(dao.findPagesByLemmaAndSiteId(lemmaDto.getLemma(), lemmaDto.getSite().getId()));
            else {
                pages = dao.findPagesByLemmaAndSiteId(lemmaDto.getLemma(), lemmaDto.getSite().getId(), pages);
            }
        }

        if (pages.isEmpty())
            Collections.emptyList();

        List<PageWithRelevance> result = new ArrayList<>();
        for (PageDto page : pages)
            result.add(new PageWithRelevance(page, dao.findIndexesByPageIdAndLemmas(page.getId(), lemmas)));

        return result.stream().map(pageWithRelevance -> {
                    PageDto page = pageWithRelevance.getPage();
                    float relRelevance = pageWithRelevance.getRelRelevance();
                    String uri = page.getPath();
                    String content = page.getContent();

                    Pattern p = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
                    Matcher matcher = p.matcher(content);
                    String title = matcher.find() ? matcher.group(1) : "Заголовок отсутствует";

                    return new PageWithRelevanceResponse(uri, title, formSnippet(content, lemmas), relRelevance);
                })
                .toList();
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase()
                .replaceAll("ё", "е")
                .replaceAll("([^а-я\\s])", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .split("\\s+");
    }

    private List<String> getLemmasFromQuery(String query) {
        return Arrays.stream(arrayContainsRussianWords(query))
                .filter(lemmaFinder::isNormalBaseWord)
                .map(lemmaFinder::getMorphLemma)
                .toList();
    }

    private String formSnippet(String content, List<String> lemmas) {
        final String pageContent = clearFromHTMLTags(removeHtmlTags(content, "script"));
        final String emptySnippet = "Пустой сниппет";

        StringBuilder formedSnippet = new StringBuilder();

        lemmas.forEach(lemma -> {
                    Pattern p = Pattern.compile("(" + lemma +")", Pattern.DOTALL);
                    Matcher matcher = p.matcher(pageContent);
                    formedSnippet.append(matcher.find() ? matcher.group(1) : "");
                }
        );

        return new StringBuilder("<b>").append(formedSnippet.isEmpty() ? emptySnippet : formedSnippet).append("</b>").toString();
    }


    private String removeHtmlTags(String text, String t) {
        final String tag = t;
        final String regExp = new StringBuilder("<").append(tag).append(">[^<].+<\\/").append(tag).append(">").toString();

        return text.replaceAll(regExp, "").trim();
    }

    private String clearFromHTMLTags(String text) {
        final String regExp = "<{1}[^>]+>{1}";
        return text.replaceAll(regExp, " ").replaceAll("\\s+", " ").replaceAll("\n", "");
    }
}

@Data
class PageWithRelevance {
    private static float max_relevance = 0;
    private final PageDto page;
    private final float absRelevance;
    private final float relRelevance;

    public PageWithRelevance(PageDto pageDto, List<IndexDto> indexes) {
        this.page = pageDto;
        this.absRelevance = indexes.stream().map(IndexDto::getRank).reduce((float) 0, Float::sum);
        max_relevance = Math.max(absRelevance, max_relevance);
        this.relRelevance = calculateRelativeRelevance();
    }

    private float calculateRelativeRelevance() {
        return absRelevance / max_relevance;
    }

    public void printRelevance() {
        System.out.println("Абсолютная релевантность: " + absRelevance);
        System.out.println("Относительная релевантность: " + relRelevance);
    }
}