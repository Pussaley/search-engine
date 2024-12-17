package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {
    public static synchronized String repairLink(String link) {
        return link.endsWith("/") ? link : link.concat("/");
    }

    public static synchronized boolean filterTest(String link) {
        link = repairLink(link);
        return String.valueOf(link.charAt(0)).equalsIgnoreCase("/") && !link.matches("\\.\\w+");
    }

    public static synchronized String parseRootURL(String link) {
        String regexp = link.contains("://www.") ? "://www." : "://";
        link = repairLink(link);

        int fromIndex = link.indexOf(regexp) + regexp.length();
        int toIndex = link.indexOf("/", fromIndex);

        return link.substring(fromIndex, toIndex);
    }

    public static boolean isSubLink(String root, String link) {
        return link.contains(root);
    }

    public synchronized static String parseRelURL(String link) {
        link = repairLink(link);
        String regexp = link.contains("://www.") ? "://www." : "://";

        String result = link.substring(link.indexOf(regexp) + regexp.length());
        return result.substring(result.indexOf("/"));
    }
}