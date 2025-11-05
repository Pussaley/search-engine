package searchengine.util.morphology;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SnippetGenerator {

    private final LemmaFinder lemmaFinder;
    private static final int MAX_SNIPPET_LENGTH = 263;
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[\\p{L}]+", Pattern.UNICODE_CHARACTER_CLASS);

    public SnippetGenerator(LemmaFinder lemmaFinder) {
        this.lemmaFinder = lemmaFinder;
    }

    public String generateSnippet(String content, String query) {
        if (content == null || content.isEmpty()) return "";

        String plainText = stripHtml(content);
        Set<String> queryLemmas = collectQueryLemmas(query);
        if (queryLemmas.isEmpty()) {
            return cropAndEscape(plainText, 0, Math.min(plainText.length(), MAX_SNIPPET_LENGTH));
        }

        List<WordPosition> allWords = findWords(plainText);
        List<WordPosition> matchedWords = allWords.stream()
                .filter(w -> lemmaMatches(w.normalizedWord, queryLemmas))
                .collect(Collectors.toList());

        if (matchedWords.isEmpty()) {
            return cropAndEscape(plainText, 0, Math.min(plainText.length(), MAX_SNIPPET_LENGTH));
        }

        int[] window = chooseBestWindow(plainText.length(), matchedWords);
        return buildHtmlSnippet(plainText, window[0], window[1], queryLemmas);
    }

    private Set<String> collectQueryLemmas(String query) {
        if (query == null || query.isBlank()) return Collections.emptySet();
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(query);

        return lemmas == null ? Collections.emptySet() : lemmas.keySet();
    }

    private List<WordPosition> findWords(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher matcher = WORD_PATTERN.matcher(lower);
        List<WordPosition> words = new ArrayList<>();

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String raw = lower.substring(start, end);
            words.add(new WordPosition(start, end, raw));
        }
        return words;
    }

    private boolean lemmaMatches(String lowerWord, Set<String> queryLemmas) {
        try {
            if (!lemmaFinder.isNormalBaseWord(lowerWord)) return false;
            String lemma = lemmaFinder.getMorphLemma(lowerWord);
            return queryLemmas.contains(lemma);
        } catch (Exception exception) {
            return queryLemmas.contains(lowerWord);
        }
    }

    private int[] chooseBestWindow(int textLength, List<WordPosition> matchedWords) {
        int bestStart = 0, bestEnd = Math.min(textLength, MAX_SNIPPET_LENGTH);
        int bestMatchCount = -1;

        for (WordPosition match : matchedWords) {
            int center = (match.start + match.end) / 2;
            int windowStart = Math.max(0, center - MAX_SNIPPET_LENGTH / 2);
            int windowEnd = Math.min(textLength, windowStart + MAX_SNIPPET_LENGTH);
            if (windowEnd - windowStart < MAX_SNIPPET_LENGTH) {
                windowStart = Math.max(0, windowEnd - MAX_SNIPPET_LENGTH);
            }

            final int ws = windowStart, we = windowEnd;
            int count = (int) matchedWords.stream()
                    .filter(w -> w.start >= ws && w.end <= we)
                    .count();

            if (count > bestMatchCount || (count == bestMatchCount && ws < bestStart)) {
                bestMatchCount = count;
                bestStart = ws;
                bestEnd = we;
            }
        }

        return new int[]{bestStart, bestEnd};
    }

    private String buildHtmlSnippet(String plain, int start, int end, Set<String> queryLemmas) {
        String window = plain.substring(start, end);
        StringBuilder stringBuilder = new StringBuilder();
        Matcher matcher = WORD_PATTERN.matcher(window);
        int last = 0;

        while (matcher.find()) {
            int ws = matcher.start(), we = matcher.end();
            stringBuilder.append(escapeHtml(window.substring(last, ws)));

            String original = window.substring(ws, we);
            boolean shouldBold = false;
            String lower = original.toLowerCase(Locale.ROOT);

            try {
                if (lemmaFinder.isNormalBaseWord(lower))
                    shouldBold = queryLemmas.contains(lemmaFinder.getMorphLemma(lower));
            } catch (Exception exception) {
                shouldBold = queryLemmas.contains(lower);
            }

            if (shouldBold)
                stringBuilder.append("<b>").append(escapeHtml(original)).append("</b>");
            else
                stringBuilder.append(escapeHtml(original));

            last = we;
        }

        stringBuilder.append(escapeHtml(window.substring(last)));
        String result = stringBuilder.toString();
        if (start > 0) result = "..." + result;
        if (end < plain.length()) result = result + "...";

        return result;
    }

    private String stripHtml(String text) {
        return text == null ? "" : text.replaceAll("<[^>]*>", " ");
    }

    private String cropAndEscape(String text, int start, int end) {
        String part = text.substring(start, end);
        if (end < text.length()) part = part + "...";

        return escapeHtml(part);
    }

    private String escapeHtml(String s) {
        if (s == null || s.isEmpty()) return s;

        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static class WordPosition {
        final int start;
        final int end;
        final String normalizedWord;

        WordPosition(int start, int end, String normalizedWord) {
            this.start = start;
            this.end = end;
            this.normalizedWord = normalizedWord;
        }
    }
}