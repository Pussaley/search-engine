package searchengine.services.jsoup;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@Slf4j
@NoArgsConstructor
public class JSOUPParser implements CommandLineRunner {

    private static final Random random = new Random();

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;

    private Optional<Document> connectToUrl(String url) {
        Document document = null;

        try {
            document = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .timeout(minTimeOut + random.nextInt(4500))
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .get();
        } catch (IOException e) {
            log.error("Ошибка при попытке парсинга страницы {}: {}", url, e.getMessage());
            throw new RuntimeException(e);
        }

        return Optional.of(document);
    }

    public void parseTagAWithHrefAttribute(String url) {
                connectToUrl(url).ifPresent(
                document -> {
                    List<String> list = document.select("a[href]")
                            .stream()
                            .filter(this::nonFile)
                            .filter(this::isInternalLink)
                            .map(element -> element.attr("href"))
                            .toList();

                    log.info("check-in: {}", list);
                }
        );
    }

    private boolean isInternalLink(String link) {
        return link.startsWith("/");
    }

    private boolean isInternalLink(Element element) {
        return isInternalLink(element.attr("href"));
    }

    private boolean nonFile(String link) {
        return Pattern
                .compile("^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^\\.]+$")
                .matcher(link)
                .matches();
    }

    private boolean nonFile(Element element) {
        return nonFile(element.attr("href"));
    }

    @Override
    public void run(String... args) throws Exception {
        String root = "https://lenta.ru/";
        parseTagAWithHrefAttribute(root);
    }
}