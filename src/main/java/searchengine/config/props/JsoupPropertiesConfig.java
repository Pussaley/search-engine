package searchengine.config.props;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import searchengine.config.props.jsoup.JsoupProperties;

@Configuration
@EnableConfigurationProperties(JsoupProperties.class)
public class JsoupPropertiesConfig {
}