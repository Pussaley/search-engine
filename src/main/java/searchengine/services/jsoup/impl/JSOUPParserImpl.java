package searchengine.services.jsoup.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import searchengine.services.jsoup.JSOUPParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class JSOUPParserImpl implements JSOUPParser<String>, CommandLineRunner {

    @Value("${jsoup-props.user-agent}")
    private String userAgent;
    @Value("${jsoup-props.referrer}")
    private String referrer;
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut;
    private static final Random random = new Random();

    public Optional<Connection.Response> getResponse(String url) {
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(minTimeOut + random.nextInt(4500))
                    .execute();
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return Optional.ofNullable(response);
    }

    public void testCSS() {
        getResponse("https://sendel.ru/").ifPresent(
                response -> {
                    Document document = null;
                    try {
                        document = response.parse();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Elements elements = document.select("a[href~=^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$]");
                    log.info("{}", elements);
                });
    }

    @Override
    public Optional<Document> connectAndGetDocument(String url) {
        Document document;
        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(minTimeOut + random.nextInt(4500))
                    .execute();

            document = response.parse();
        } catch (IOException e) {
            log.error("Ошибка при попытке парсинга страницы {}: {}", url, e.getMessage());
            throw new RuntimeException(e);
        }

        return Optional.of(document);
    }

    public List<String> parseAbsoluteURLs(String url) {

        List<String> absoluteURLs = new ArrayList<>();

        connectAndGetDocument(url).ifPresent(document -> document.select("a[href]")
                .stream()
                .map(element -> element.attr("abs:href"))
                .filter(this::nonFile)
                .forEachOrdered(absoluteURLs::add));

        return absoluteURLs;
    }

    private boolean nonFile(String link) {
        return java.util.regex.Pattern
                .compile("^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$")
                .matcher(link)
                .matches();
    }

    @Override
    public void run(String... args) throws Exception {
        testCSS();
    }
}