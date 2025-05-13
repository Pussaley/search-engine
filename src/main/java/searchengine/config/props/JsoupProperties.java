package searchengine.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.search-engine")
@Data
public class JsoupProperties {
    private String userAgent;
    private String referrer;
    private int minTimeOut;
}
