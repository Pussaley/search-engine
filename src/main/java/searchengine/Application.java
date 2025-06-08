package searchengine;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import searchengine.util.jsoup.JSOUPParser;

@SpringBootApplication
@Slf4j
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    @PreDestroy
    public void preDestroy() {
        log.info("Некорректные ссылки:\n\t\t\t {}", JSOUPParser.incorrectLinks);
    }
}