package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {
    public static synchronized String getRelURL(String url) {
        String root = parseRootURL(url);
        return url.split(root)[1];
    }

    public static synchronized String parseRootURL(String link) {
        String httpRegexp = "://";

        if (!link.endsWith("/"))
            link = link.concat("/");
        int index = link.indexOf(httpRegexp);
        int fromIndex = index + httpRegexp.length();
        int secIndex = link.indexOf("/", fromIndex);
//        log.info("Link: {}", link);
        String result = link.substring(fromIndex, secIndex);
//        log.info("Result: {}", result);
        return result;
    }
}