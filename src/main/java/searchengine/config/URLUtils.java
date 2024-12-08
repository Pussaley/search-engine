package searchengine.config;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@NoArgsConstructor
public class URLUtils {

    public boolean splitter(String absoluteURL) {
        if (!absoluteURL.endsWith("/"))
            absoluteURL = absoluteURL.concat("/");
        String[] split = absoluteURL.split("://");
        String[] root = split[1].split("/");
        return true;
    }
}