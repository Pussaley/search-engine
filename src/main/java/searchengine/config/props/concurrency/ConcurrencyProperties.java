package searchengine.config.props.concurrency;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.search-engine.concurrency")
@Data
public class ConcurrencyProperties {
    private int shutdownTimeout;
}