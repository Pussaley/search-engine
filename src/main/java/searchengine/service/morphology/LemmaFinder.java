package searchengine.service.morphology;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class LemmaFinder {

    private final LuceneMorphology morphology;
    private final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    public static LemmaFinder getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new LemmaFinder(morphology);
    }

    private LemmaFinder(LuceneMorphology morphology) {
        this.morphology = morphology;
    }

    public synchronized Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsRussianWords(clearFromHTMLTags(text));

        HashMap<String, Integer> collect = Arrays.stream(words)
                .filter(this::isNormalBaseWord)
                .map(word -> morphology.getNormalForms(word).get(0))
                .collect(Collectors.toMap(Function.identity(), a -> 1, Integer::sum, HashMap::new));

        return collect;
    }

    private boolean isNormalBaseWord(String word) {
        return morphology.getMorphInfo(word).stream()
                .noneMatch(s -> Arrays.stream(particlesNames).anyMatch(s::contains));
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase().replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private String clearFromHTMLTags(String text) {
        final String regExp = "<{1}[^>]+>{1}";
        return text.replaceAll(regExp, "").replaceAll("\\t", " ");
    }
}