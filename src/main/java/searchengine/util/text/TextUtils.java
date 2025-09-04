package searchengine.util.text;

import org.springframework.stereotype.Component;

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
}