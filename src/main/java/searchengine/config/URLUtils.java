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

        int index = link.indexOf(httpRegexp);
        int fromIndex = index + httpRegexp.length();
        int secIndex = link.indexOf("/", fromIndex);

        return link.substring(fromIndex, secIndex);
    }

//    private synchronized String getRootURL(String absoluteURL) {
//        String[] split = absoluteURL.split("://");
//        return split[1].split("/")[0];
//    }
//    private synchronized String getRootURL(Site site) {
//        return getRootURL(site.getUrl());
//    }
}