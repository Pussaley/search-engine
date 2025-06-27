package searchengine.config.props.jsoup;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.config.props.jsoup.JsoupHeader;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "app.search-engine.jsoup")
@Data
public class JsoupProperties {
    private String userAgent;
    private String referrer;
    private int minTimeOut;
    private List<JsoupHeader> headers;

    public Map<String, String> headersToMap() {
        return headers.stream().collect(Collectors.toMap(
                JsoupHeader::key,
                JsoupHeader::value));
    }
}