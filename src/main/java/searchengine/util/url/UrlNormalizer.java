package searchengine.util.url;

import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class UrlNormalizer {

    private final String http = "http://";
    private final String regExp = "^[a-zA-Z]+://.*";

    public String normalize(String input) {
        try {
            String fixedInput = input.matches(regExp) ? input : http + input;
            URI uri = URI.create(fixedInput);
            String host = uri.getHost();
            return host != null ? host.toLowerCase() : input.toLowerCase();
        } catch (Exception exception) {
            return input.toLowerCase();
        }
    }

    public String appendTrailingSlashIfNeeded(String url) {
        return url.endsWith(".html") ? url : url.endsWith("/") ? url : url.concat("/");
    }
}