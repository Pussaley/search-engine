package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {
    public static synchronized String repairLink(String link) {
        return link.endsWith("/") ? link.substring(0, link.length() - 1) : link ;
    }

    public static synchronized String parseRootURL(String link) {
        String regexp = getRegExp(link);
        link = repairLink(link);

        int fromIndex = link.indexOf(regexp) + regexp.length();

        return link.substring(fromIndex);
    }

    public static boolean isSubLink(String root, String link) {
        return link.contains(root);
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
}