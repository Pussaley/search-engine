package searchengine.services.jsoup;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class JSOUPParser {

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;
    private static final Random random = new Random();
    private static final List<String> test = new ArrayList<>();

    private Optional<Document> connectAndGetDocument(String url) {
        Document document;

        try {
            document = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(minTimeOut + random.nextInt(4500))
                    .get();
        } catch (IOException e) {
            log.error("Ошибка при попытке парсинга страницы {}: {}", url, e.getMessage());
            throw new RuntimeException(e);
        }

        return Optional.of(document);
    }

    public List<String> parseAbsoluteURLs(String url) {

        List<String> absoluteURLs = new ArrayList<>();

        connectAndGetDocument(url).ifPresent(document -> {

            document.select("a[href]")
                    .stream()
                    .map(element -> element.attr("abs:href"))
                    .filter(this::nonFile)
                    .filter(this::splitter)
                    .limit(100)
                    .forEachOrdered(absoluteURLs::add);
            log.info("check-in: {}", absoluteURLs);

        });

        return absoluteURLs;
    }

    private boolean nonFile(String link) {

        return java.util.regex.Pattern
                .compile("^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$")
                .matcher(link)
                .matches();
    }

    private boolean splitter(String absoluteURL) {
        if (!absoluteURL.endsWith("/"))
            absoluteURL = absoluteURL.concat("/");
        String[] split = absoluteURL.split("://");
        String[] root = split[1].split("/");
        if (!test.contains(root[0])) {
            test.add(root[0]);
        }
        return true;
    }
}