package searchengine.util.url;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {

    public static synchronized boolean filterCorrectLinks(String link) {
        return notFile(link);
    }

    public static synchronized String removeEndBackslash(String link) {
        return getLastChar(link).equalsIgnoreCase("/")
                ? link.substring(0, link.length() - 1) : link;
    }

    public static synchronized String repairLink(String link) {
        if (!link.endsWith("/"))
            link = link.concat("/");

        return remove3w(removeAnchors(link));
    }

    private synchronized static String getLastChar(String link) {
        return link.substring(link.length() - 1);
    }

    public static synchronized String parseRootURL(String link) {
        String regexp = getRegExp(link);
        link = repairLink(link);

        int beginIndex = link.indexOf(regexp) + regexp.length();
        int endIndex = link.indexOf("/", beginIndex) == -1 ? link.length() - 1 : link.indexOf("/", beginIndex);

        return link.substring(beginIndex, endIndex);
    }

    public synchronized static String parseRelURL(String link) {
        if (link.startsWith("/"))
            return link;

        link = repairLink(link);
        String regexp = getRegExp(link);

        String result = link.substring(link.indexOf(regexp) + regexp.length());
        return result.substring(result.indexOf("/"));
    }

    private synchronized static String getRegExp(String link) {
        return link.contains("://www.")
                ? "://www." : link.contains("://") ? "://" : "/";
    }

    public synchronized static boolean isSubLink(String root, String l) {
        return l.contains(root);
    }

    private static synchronized boolean notFile(String link) {
        return link.matches(".*\\/[^\\.]+\\z");
    }

    private static synchronized String removeAnchors(String link) {
        return link.contains("#") ? link.substring(0, link.indexOf("#")) : link;
    }

    private static synchronized String remove3w(String link) {
        return link.contains("www.") ? link.replace("www.", "") : link;
    }

    public static String shortLink(String link) {
        return remove3w(link);
    }
}