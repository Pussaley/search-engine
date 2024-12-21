package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {

    public static synchronized boolean filterCorrectLinks(String link) {
        String result = removeAnchors(link);
        result = removeEndBackslash(result);
        return notFile(result);
    }
    private static synchronized String removeEndBackslash(String link) {
        return getLastChar(link).equalsIgnoreCase("/")
                ? link.substring(0, link.length() - 1)
                : link ;
    }

    public static synchronized String repairLink(String link) {
        return removeEndBackslash(link);
    }

    public static synchronized boolean isMarkedURL(String link) {
        return link.startsWith("#");
    }

    public static synchronized boolean notMarkedURL(String link) {
        return !isMarkedURL(link);
    }

    private synchronized static String getLastChar(String link) {
        return link.substring(link.length() - 1);
    }

    public static synchronized String parseRootURL(String link) {
        String regexp = getRegExp(link);
        link = repairLink(link);

        int fromIndex = link.indexOf(regexp) + regexp.length();

        return link.substring(fromIndex);
    }

    public synchronized static String parseRelURL(String link) {
        if (link.startsWith("/"))
            return link;

        link = repairLink(link);
        String regexp = getRegExp(link);

        String result = link.substring(link.indexOf(regexp) + regexp.length());
        return result.substring(result.indexOf("/"));
    }

    public synchronized static boolean isMainURL(String link) {
        if (link.startsWith("/") && link.length() > 1)
            return false;

        link = parseRelURL(link);
        return link.equalsIgnoreCase("/");
    }

    public synchronized static boolean notMainURL(String link) {
        return !isMainURL(link);
    }

    private synchronized static String getRegExp(String link) {
        return link.contains("://www.")
                ? "://www."
                : link.contains("://")
                ? "://"
                : "/";
    }

    public synchronized static boolean isSubLink(String parsedRootURL, String l) {
        return l.contains(parsedRootURL);
    }


    private static synchronized boolean notFile(String link) {
        return !link.matches(".*\\/.*\\.(.)*\\z");
    }

    private static synchronized String removeAnchors(String link) {
        return link.contains("#") ? link.substring(0, link.indexOf("#")) : link;
    }
}