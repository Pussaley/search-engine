package searchengine.config.props;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import searchengine.config.props.concurrency.ConcurrencyProperties;

@Configuration
@EnableConfigurationProperties(ConcurrencyProperties.class)
public class ConcurrencyPropertiesConfig {
}