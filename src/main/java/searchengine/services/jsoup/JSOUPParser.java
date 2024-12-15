package searchengine.services.jsoup;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.URLUtils;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
@Data
public class JSOUPParser {

    @Value("${jsoup-props.user-agent}")
    private String userAgent; // = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    @Value("${jsoup-props.referrer}")
    private String referrer; // = "https://www.google.com/";
    @Value("${jsoup-props.min-time-out}")
    private int minTimeOut; // = 500;
    private static final Random random = new Random();
    private final URLUtils utils;

    public JSOUPParser() {
        this.utils = new URLUtils();
    }

    public Connection.Response executeRequest(String url) {
        int timeOut = minTimeOut + random.nextInt(4500);
        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timeout(timeOut)
                    .execute();
        } catch (SocketTimeoutException socketTimeoutException) {
            log.error("SocketTimeoutException were thrown while executing the request to {}", url);
            log.error("Error: {}", socketTimeoutException.getCause());
        } catch (IOException e) {
            log.error("Error while executing request to: {}", url);
            log.error("Error: {}", e.getMessage());
        }

        return response;
    }

    public Document connectToUrlAndGetDocument(String url) {
        Document document;
        try {
            document = executeRequest(url).parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return document;
    }

    public Collection<String> parseAbsoluteLinks(String url) {
        String parsedRootURL = URLUtils.parseRootURL(url);
        return connectToUrlAndGetDocument(url).select("a[href~=^((http(s)?(\\:{1,2})\\/*)?[\\w\\.\\-]+)?\\/?[^(\\.\\#)]+$]")
                .stream()
                .map(element -> element.attr("abs:href"))
                .filter(link -> link.contains(parsedRootURL))
                .collect(Collectors.toSet());
    }

    private boolean filterTest(String link) {
        return String.valueOf(link.charAt(0)).equalsIgnoreCase("/") && !link.matches("\\.\\w+");
    }
}