package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {

    private static synchronized String checkTest(String link) {
        return link.endsWith("/") ? link : link.concat("/");
    }

    public static synchronized boolean filterTest(String link) {
        link = checkTest(link);
        return String.valueOf(link.charAt(0)).equalsIgnoreCase("/") && !link.matches("\\.\\w+");
    }

    public static synchronized String parseRootURL(String link) {
        String regexp = link.contains("://www.") ? "://www." : "://";
        link = checkTest(link);

        int fromIndex = link.indexOf(regexp) + regexp.length();
        int toIndex = link.indexOf("/", fromIndex);

        return link.substring(fromIndex, toIndex);
    }
}