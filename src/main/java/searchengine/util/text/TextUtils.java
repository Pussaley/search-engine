package searchengine.util.text;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TextUtils {
    public String clearFromHTMLTags(String text) {
        final String regExp = "<{1}[^>]+>{1}";
        return text.replaceAll(regExp, " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\n", "")
                .replaceAll("([^A-zА-я0-9\\s])", " ");
    }

    public String removeHtmlTags(String text, String t) {
        final String tag = t;
        final String regExp = new StringBuilder("<").append(tag).append(".+>[^<].+<\\/").append(tag).append(">").toString();

        return text.replaceAll(regExp, "").trim();
    }

    public String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase()
                .replaceAll("ё", "е")
                .replaceAll("([^а-я\\s])", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .split("\\s+");
    }

    public String formTitle(String content) {
        Matcher matcher = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL).matcher(content);
        return matcher.find() ? matcher.group(1) : "Заголовок отсутствует";
    }
}