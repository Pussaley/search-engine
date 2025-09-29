package searchengine.service.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.dto.response.search.PageWithRelevanceResponse;
import searchengine.model.dto.search.PageWithRelevance;
import searchengine.model.entity.dto.IndexDto;
import searchengine.model.entity.dto.LemmaDto;
import searchengine.model.entity.dto.PageDto;
import searchengine.repository.DataSearchDAO;
import searchengine.service.search.demo.relevance.Demo;
import searchengine.util.morphology.LemmaFinder;
import searchengine.util.text.TextUtils;

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
    private final TextUtils textUtils;

    public Map<Site, List<PageWithRelevanceResponse>> searchAsMap(String query, String siteUrl) {
        return sites.getSites().stream()
                .filter(site -> site.getUrl().equalsIgnoreCase(siteUrl))
                .findFirst()
                .stream()
                .collect(Collectors.toMap(Function.identity(),
                        (site) -> searchAsList(query, site.getUrl())));
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

    public List<?> test(String query, String siteUrl) {


        List<String> lemmas = getLemmasFromQuery(query);
        Long siteId = dao.getSiteIdBySiteUrl(siteUrl);

        List<LemmaDto> foundLemmaDtos = new ArrayList<>();
        lemmas.forEach(l -> dao.findLemmasByLemmaAndSiteId(l, siteId).ifPresent(
                lemmaDto -> {
                    Integer pagesCount = dao.countPagesBySiteId(siteId);
                    if (lemmaDto.getFrequency() < pagesCount * 0.75)
                        foundLemmaDtos.add(lemmaDto);
                })
        );
        List<IndexDto> indexes = new ArrayList<>();
        foundLemmaDtos.forEach(l -> indexes.addAll(dao.findIndexesByLemmaId(l.getId())));

        Demo demo = new Demo(Map.of());

        return List.of();
    }

    public List<PageWithRelevanceResponse> searchAsList(String query, String siteUrl) {

        List<String> lemmas = getLemmasFromQuery(query);

        Long siteId = dao.getSiteIdBySiteUrl(siteUrl);
        List<LemmaDto> resultLemmas = new ArrayList<>();
        lemmas.forEach(l -> dao.findLemmasByLemmaAndSiteId(l, siteId).ifPresent(
                lemmaDto -> {
                    Integer pagesCount = dao.countPagesBySiteId(siteId);
                    if (lemmaDto.getFrequency() < pagesCount * 0.75)
                        resultLemmas.add(lemmaDto);
                })
        );

        if (resultLemmas.isEmpty()) return Collections.emptyList();

        Set<LemmaDto> sortedLemmas = new TreeSet<>(Comparator.comparing(LemmaDto::getFrequency));
        sortedLemmas.addAll(resultLemmas);

        LemmaDto[] lemmaDtosArray = sortedLemmas.toArray(LemmaDto[]::new);
        List<PageDto> pages = new ArrayList<>();
        for (int i = 0; i < lemmaDtosArray.length; i++) {
            LemmaDto lemmaDto = lemmaDtosArray[i];
            if (i == 0)
                pages.addAll(dao.findPagesByLemmaAndSiteId(lemmaDto.getLemma(), lemmaDto.getSite().getId()));
            else
                pages = dao.findPagesByLemmaAndSiteId(lemmaDto.getLemma(), lemmaDto.getSite().getId(), pages);
        }

        if (pages.isEmpty()) return Collections.emptyList();

        List<PageWithRelevance> result = new ArrayList<>();
        for (PageDto page : pages)
            result.add(new PageWithRelevance(page, dao.findIndexesByPageIdAndLemmas(page.getId(), lemmas)));

        return result.stream().map(pageWithRelevance -> {
                    PageDto page = pageWithRelevance.getPage();
                    float relRelevance = pageWithRelevance.getRelRelevance();
                    String content = page.getContent();

                    return new PageWithRelevanceResponse(
                            page.getPath(),
                            textUtils.formTitle(content),
                            formSnippetTest(content, query),
                            relRelevance);
                })
                .toList();
    }

    private List<String> getLemmasFromQuery(String query) {
        return Arrays.stream(textUtils.arrayContainsRussianWords(query))
                .filter(lemmaFinder::isNormalBaseWord)
                .map(lemmaFinder::getMorphLemma)
                .toList();
    }
}