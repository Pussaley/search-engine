package searchengine.config.morphology;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.util.morphology.LemmaFinder;

import java.io.IOException;

@Configuration
public class MorphologyConfig {

    @Bean
    public LemmaFinder lemmaFinder() throws IOException {
        return LemmaFinder.getInstance();
    }
}