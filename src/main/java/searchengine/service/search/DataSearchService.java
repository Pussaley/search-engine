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
import searchengine.util.text.TextUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    private final TextUtils textUtils;
    private final double MAX_LEMMA_FREQUENCY_PERCENTAGE = 0.75;

    public Map<Site, List<PageWithRelevanceResponse>> searchAsMap(String query, String siteUrl) {
        return sites.getSites().stream()
                .filter(site -> site.getUrl().equalsIgnoreCase(siteUrl))
                .findFirst().stream()
                .collect(Collectors.toMap(Function.identity(),
                        (site) -> findRelevancedPages(query, site.getUrl())));
    }

    private String formSnippetTest(String content, String query) {

        final String emptySnippet = "Пустой сниппет";
        final String openingBTag = "<b>";
        final String closingBTag = "</b>";

        String[] queryWords = query.split("\\s+");
        String resultContent = textUtils.clearFromHTMLTags(textUtils.removeHtmlTags(content, "script"));

        StringBuilder stringBuilder = new StringBuilder();

        for (String qWord : queryWords) {
            if (qWord.length() < 3) continue;

            final String regexp = "\\b".concat(qWord.substring(0, qWord.length() - 2)).concat(".*?(?=$|\\s|[.,!?])");
            Matcher matcher = Pattern
                    .compile(regexp, Pattern.UNICODE_CHARACTER_CLASS | Pattern.CASE_INSENSITIVE)
                    .matcher(resultContent);

            while (matcher.find()) {
                String matched = matcher.group();

                if (lemmaFinder.getMorphLemma(matched).equalsIgnoreCase(lemmaFinder.getMorphLemma(qWord))) {
                    String replacement = openingBTag.concat(matched).concat(closingBTag);
                    matcher.appendReplacement(stringBuilder, replacement);
                }
            }

            matcher.appendTail(stringBuilder);
            resultContent = stringBuilder.toString();
            stringBuilder = new StringBuilder();
        }

        return !resultContent.isEmpty() ? processText(resultContent) : emptySnippet;
    }

    public static String processText(String text) {

        final int snippetLength = 280;

        if (text == null || text.isEmpty()) return text;

        Matcher matcher = Pattern.compile("<b>(.*?)</b>").matcher(text);

        List<MatchResult> matches = matcher.results()
                .map(mr -> new MatchResult(mr.start(), mr.end(), mr.group(1), mr.group()))
                .collect(Collectors.toList());

        if (matches.isEmpty())
            return text.length() <= snippetLength ? text : text.substring(0, snippetLength - 3) + "...";

        int totalHighlightedLength = matches.stream()
                .mapToInt(match -> match.fullTag.length())
                .sum();

        int availableSpace = snippetLength - totalHighlightedLength;

        if (availableSpace <= 0) return truncateHighlightedText(matches, snippetLength);

        int gapCount = Math.min(3, matches.size() + 1);
        int spacePerGap = availableSpace / gapCount;

        StringBuilder result = new StringBuilder();
        int currentPosition = 0;

        MatchResult firstMatch = matches.get(0);
        if (firstMatch.start > currentPosition) {
            String beforeFirst = text.substring(currentPosition, firstMatch.start);
            result.append(processTextSegment(beforeFirst, spacePerGap, true, false));
        }
        result.append(firstMatch.fullTag);
        currentPosition = firstMatch.end;

        for (int i = 1; i < matches.size(); i++) {
            MatchResult currentMatch = matches.get(i);
            String between = text.substring(currentPosition, currentMatch.start);

            result.append(processTextSegment(between, spacePerGap * 2, false, false));
            result.append(currentMatch.fullTag);

            currentPosition = currentMatch.end;
        }

        if (currentPosition < text.length()) {
            String afterLast = text.substring(currentPosition);
            result.append(processTextSegment(afterLast, spacePerGap, false, true));
        }

        String finalResult = result.toString();
        if (finalResult.length() > snippetLength) return finalResult.substring(0, snippetLength - 3) + "...";

        return finalResult;
    }

    private static String processTextSegment(String segment, int maxLength, boolean isStart, boolean isEnd) {
        if (segment.length() <= maxLength) return segment;

        if (maxLength <= 6) return "...";

        if (isStart)
            return "..." + segment.substring(segment.length() - maxLength + 3);
        else if (isEnd)
            return segment.substring(0, maxLength - 3) + "...";
        else {
            int halfLength = maxLength / 2;
            String startPart = segment.substring(0, halfLength - 3);
            String endPart = segment.substring(segment.length() - halfLength + 3);
            return startPart + "..." + endPart;
        }
    }

    private static String truncateHighlightedText(List<MatchResult> matches, int maxLength) {
        StringBuilder result = new StringBuilder();
        int remainingLength = maxLength;

        for (MatchResult match : matches) {
            if (remainingLength <= 3) {
                result.append("...");
                break;
            }

            if (match.fullTag.length() <= remainingLength) {
                result.append(match.fullTag);
                remainingLength -= match.fullTag.length();
            } else {
                String truncated = match.fullTag.substring(0, remainingLength - 3) + "...";
                result.append(truncated);
                break;
            }
        }

        return result.toString();
    }

    private static class MatchResult {
        int start;
        int end;
        String content;
        String fullTag;

        MatchResult(int start, int end, String content, String fullTag) {
            this.start = start;
            this.end = end;
            this.content = content;
            this.fullTag = fullTag;
        }
    }

    public List<PageWithRelevanceResponse> findRelevancedPages(String query, String siteUrl) {

        List<String> lemmas = getLemmasFromQuery(query);
        Long siteId = dao.getSiteIdBySiteUrl(siteUrl);
        Integer pagesCount = dao.countPagesBySiteId(siteId);

        List<LemmaDto> list = findLemmaDtosSortedByFrequency(siteId, lemmas, pagesCount);

        if (list.isEmpty()) return Collections.emptyList();

        Iterator<LemmaDto> it = list.iterator();
        List<PageDto> pages = dao.findPagesByLemmaId(it.next().getId());

        while (it.hasNext())
            pages = dao.findPagesByLemmaIdInPages(it.next().getId(), pages.stream().map(PageDto::getId).toList());

        return pages.isEmpty() ? Collections.emptyList() : convertPagesIntoRelevancedPages(query, pages, lemmas);
    }

    private List<PageWithRelevanceResponse> convertPagesIntoRelevancedPages(String query, List<PageDto> pages, List<String> lemmas) {
        List<PageWithRelevance> result = pages.stream()
                .map( page -> new PageWithRelevance(page, dao.findIndexesByPageIdAndLemmas(page.getId(), lemmas))
                ).toList();

        return result.stream()
                .map(pageWithRelevance -> {
                    String content = pageWithRelevance.getPage().getContent();

                    return new PageWithRelevanceResponse(
                            pageWithRelevance.getPage().getPath(),
                            textUtils.formTitle(content),
                            formSnippetTest(content, query),
                            pageWithRelevance.getRelRelevance());})
                .toList();
    }

    private List<LemmaDto> findLemmaDtosSortedByFrequency(Long siteId, List<String> lemmas, Integer pagesCount) {
        List<LemmaDto> lemmasOrderedByFrequency =
                dao.findLemmasOrderByFrequency(siteId, lemmas);

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